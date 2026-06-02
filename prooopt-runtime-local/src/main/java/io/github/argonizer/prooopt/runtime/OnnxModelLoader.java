/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.exception.PrOOPtExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.LongBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The core local-inference engine. Manages a single ONNX Runtime session plus a DJL Hugging Face
 * tokenizer, and runs greedy autoregressive decoding to produce a full text completion.
 *
 * <p>Loading happens off the calling thread via {@link #loadAsync()} on a virtual thread; callers
 * block only on first use via {@link #awaitReady()} (60-second cap). A load failure is captured and
 * rethrown from the first {@link #infer(String)} call rather than being silently swallowed.
 *
 * <p>v0.1.0 implements greedy (argmax) decoding only — no beam search, top-k/top-p sampling, or
 * streaming. The prompt template targets the Phi-3.5 Mini chat format; other models may need a
 * different template.
 */
public class OnnxModelLoader implements AutoCloseable {

    private static final Logger log = LogManager.getLogger("io.github.argonizer.prooopt.audit");

    private final LocalModelConfig config;

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    private GenerationConfig generationConfig;

    private volatile boolean ready = false;
    private volatile Throwable loadFailure;
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    /** How long {@link #awaitReady()} blocks before giving up. Package-private for tests. */
    volatile long readyTimeoutMs = 60_000;

    public OnnxModelLoader(LocalModelConfig config) {
        this.config = config;
    }

    // ------------------------------------------------------------------ loading

    /** Loads the model and tokenizer on a virtual thread. Returns immediately; idempotent. */
    public void loadAsync() {
        Thread.ofVirtual().name("prooopt-onnx-load").start(this::load);
    }

    /** The actual load body. Package-private so tests can drive it on the calling thread. */
    void load() {
        try {
            log.info("[PROOOPT][ONNX][LOADING] modelPath={} tokenizerPath={}",
                    config.getModelPath(), config.getTokenizerPath());

            this.env = createEnvironment();

            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            opts.setIntraOpNumThreads(config.getIntraThroughputThreads());

            this.session = createSession(env, opts);
            this.tokenizer = createTokenizer();
            this.generationConfig = loadGenerationConfig(config.getGenerationConfigPath());

            this.ready = true;
            log.info("[PROOOPT][ONNX][READY] model={} maxTokens={} thinking={}",
                    new File(config.getModelPath()).getName(), config.getMaxTokens(), config.isThinking());
        } catch (Throwable t) {
            this.loadFailure = t;
            log.error("[PROOOPT][ONNX][LOAD_FAILED] error='{}'", t.getMessage());
        } finally {
            readyLatch.countDown();
        }
    }

    // ---- Overridable creation seams (kept simple so tests can supply mocks) ----

    OrtEnvironment createEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    OrtSession createSession(OrtEnvironment environment, OrtSession.SessionOptions opts) throws OrtException {
        return environment.createSession(config.getModelPath(), opts);
    }

    HuggingFaceTokenizer createTokenizer() throws Exception {
        return HuggingFaceTokenizer.newInstance(Paths.get(config.getTokenizerPath()));
    }

    // ------------------------------------------------------------------ inference

    /** Runs greedy decoding for {@code prompt} and returns the full generated completion. */
    public String infer(String prompt) {
        awaitReady();

        String wrappedPrompt = applyPromptTemplate(prompt, config.isThinking());
        int maxNewTokens = generationConfig.resolvedMaxNewTokens(config.getMaxTokens());
        log.info("[PROOOPT][ONNX][INFER_START] promptLength={} maxNewTokens={}",
                wrappedPrompt.length(), maxNewTokens);
        long start = System.currentTimeMillis();

        Encoding encoding = tokenizer.encode(wrappedPrompt, true, false);
        long[] inputIds = toLongArray(encoding.getIds());

        List<Long> generatedIds = new ArrayList<>();
        long[] currentIds = inputIds;
        int eosTokenId = generationConfig.resolvedEosTokenId();

        try {
            for (int step = 0; step < maxNewTokens; step++) {
                long[] shape = {1L, currentIds.length};

                try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                             env, LongBuffer.wrap(currentIds), shape);
                     OnnxTensor maskTensor = OnnxTensor.createTensor(
                             env, LongBuffer.wrap(onesLong(currentIds.length)), shape)) {

                    Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
                    inputs.put("input_ids", inputTensor);
                    inputs.put("attention_mask", maskTensor);

                    try (OrtSession.Result result = session.run(inputs)) {
                        // Logits: shape [batch=1, seq_len, vocab_size]
                        float[][][] logits = (float[][][]) result.get(0).getValue();
                        float[] lastLogits = logits[0][logits[0].length - 1];
                        long nextToken = argmax(lastLogits);

                        if (nextToken == eosTokenId) {
                            break;
                        }
                        generatedIds.add(nextToken);
                        currentIds = appendLong(currentIds, nextToken);
                    }
                }
            }
        } catch (OrtException e) {
            throw new PrOOPtExecutionException("ONNX", "infer", "ONNX inference failed", e);
        }

        String output = tokenizer.decode(toPrimitive(generatedIds), true);
        long duration = System.currentTimeMillis() - start;
        log.info("[PROOOPT][ONNX][INFER_END] generatedTokens={} duration={}ms",
                generatedIds.size(), duration);
        return output.strip();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Wraps a prompt in the Phi-3.5 Mini chat template. Other models may require a different
     * template; override or extend for those.
     */
    String applyPromptTemplate(String prompt, boolean thinking) {
        if (thinking) {
            return "<|system|>Think step by step.<|end|>"
                    + "<|user|>" + prompt + "<|end|><|assistant|>";
        }
        return "<|user|>" + prompt + "<|end|><|assistant|>";
    }

    /** Blocks until the model is ready, or throws if the load failed or timed out. */
    void awaitReady() {
        if (!ready) {
            try {
                boolean signalled = readyLatch.await(readyTimeoutMs, TimeUnit.MILLISECONDS);
                if (loadFailure != null) {
                    throw new PrOOPtConfigException(
                            "ONNX model failed to load: " + loadFailure.getMessage(), loadFailure);
                }
                if (!signalled || !ready) {
                    throw new PrOOPtConfigException(
                            "ONNX model did not load within " + readyTimeoutMs + "ms");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PrOOPtConfigException("interrupted while waiting for ONNX model to load", e);
            }
        }
        if (loadFailure != null) {
            throw new PrOOPtConfigException(
                    "ONNX model failed to load: " + loadFailure.getMessage(), loadFailure);
        }
    }

    /** Loads {@code generation_config.json}, or returns defaults when the path is null/missing. */
    GenerationConfig loadGenerationConfig(String path) {
        if (path == null || path.isBlank() || !new File(path).isFile()) {
            return new GenerationConfig();
        }
        try {
            return new ObjectMapper().readValue(new File(path), GenerationConfig.class);
        } catch (Exception e) {
            throw new PrOOPtConfigException("failed to parse generation_config.json at " + path, e);
        }
    }

    static long[] toLongArray(long[] ids) {
        return Arrays.copyOf(ids, ids.length);
    }

    static long[] toLongArray(int[] ids) {
        long[] out = new long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            out[i] = ids[i];
        }
        return out;
    }

    static long[] onesLong(int length) {
        long[] ones = new long[length];
        Arrays.fill(ones, 1L);
        return ones;
    }

    static long[] appendLong(long[] arr, long value) {
        long[] out = Arrays.copyOf(arr, arr.length + 1);
        out[arr.length] = value;
        return out;
    }

    static long argmax(float[] logits) {
        int bestIndex = 0;
        float best = logits[0];
        for (int i = 1; i < logits.length; i++) {
            if (logits[i] > best) {
                best = logits[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static long[] toPrimitive(List<Long> list) {
        long[] out = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    // ------------------------------------------------------------------ accessors / lifecycle

    public boolean isReady() {
        return ready;
    }

    public HuggingFaceTokenizer getTokenizer() {
        return tokenizer;
    }

    public OrtSession getSession() {
        return session;
    }

    public OrtEnvironment getEnv() {
        return env;
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            throw new PrOOPtExecutionException("ONNX", "close", "failed to release ONNX resources", e);
        } finally {
            log.info("[PROOOPT][ONNX][CLOSED] session released");
        }
    }
}
