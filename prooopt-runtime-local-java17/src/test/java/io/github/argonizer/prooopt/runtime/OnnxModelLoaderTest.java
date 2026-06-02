/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OnnxModelLoaderTest {

    private static LocalModelConfig config() {
        LocalModelConfig c = new LocalModelConfig();
        c.setModelPath("model.onnx");
        c.setTokenizerPath("tokenizer.json");
        return c;
    }

    /** Loader whose creation seams succeed without touching disk or native libs. */
    private static OnnxModelLoader succeedingLoader() {
        return new OnnxModelLoader(config()) {
            @Override
            OrtEnvironment createEnvironment() {
                return mock(OrtEnvironment.class);
            }

            @Override
            OrtSession createSession(OrtEnvironment env, OrtSession.SessionOptions opts) {
                return mock(OrtSession.class);
            }

            @Override
            ai.djl.huggingface.tokenizers.HuggingFaceTokenizer createTokenizer() {
                return null; // not exercised by these tests
            }
        };
    }

    @Test
    void loadAsync_setsReadyTrue_afterSuccessfulLoad() throws InterruptedException {
        OnnxModelLoader loader = succeedingLoader();
        loader.loadAsync();

        long deadline = System.currentTimeMillis() + 2000;
        while (!loader.isReady() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(loader.isReady());
    }

    @Test
    void loadAsync_storesException_onLoadFailure() {
        OnnxModelLoader loader = new OnnxModelLoader(config()) {
            @Override
            OrtEnvironment createEnvironment() {
                throw new RuntimeException(newOrtException());
            }
        };
        loader.readyTimeoutMs = 2000;
        loader.loadAsync();

        PrOOPtConfigException ex = assertThrows(PrOOPtConfigException.class, () -> loader.infer("test"));
        // The OrtException is carried as the cause chain root.
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof OrtException
                || (cause != null && cause.getCause() instanceof OrtException)
                || cause instanceof RuntimeException);
    }

    @Test
    void infer_throwsConfigException_ifNotReady() {
        OnnxModelLoader loader = new OnnxModelLoader(config());
        loader.readyTimeoutMs = 10; // do NOT call loadAsync()
        assertThrows(PrOOPtConfigException.class, () -> loader.infer("test"));
    }

    @Test
    void applyPromptTemplate_thinking_false_wrapsCorrectly() {
        OnnxModelLoader loader = new OnnxModelLoader(config());
        assertEquals("<|user|>Hello<|end|><|assistant|>",
                loader.applyPromptTemplate("Hello", false));
    }

    @Test
    void applyPromptTemplate_thinking_true_includesSystemPrompt() {
        OnnxModelLoader loader = new OnnxModelLoader(config());
        String result = loader.applyPromptTemplate("Hello", true);
        assertTrue(result.startsWith("<|system|>Think step by step.<|end|>"));
        assertTrue(result.contains("<|user|>Hello<|end|><|assistant|>"));
    }

    @Test
    void argmax_returnsCorrectIndex() {
        float[] logits = {0.1f, 0.9f, 0.3f, 0.2f};
        assertEquals(1, OnnxModelLoader.argmax(logits));
    }

    @Test
    void argmax_handlesLargeVocab() {
        float[] logits = new float[32000];
        logits[17842] = 1.0f;
        assertEquals(17842, OnnxModelLoader.argmax(logits));
    }

    @Test
    void onesLong_returnsCorrectArray() {
        long[] ones = OnnxModelLoader.onesLong(5);
        assertEquals(5, ones.length);
        for (long v : ones) {
            assertEquals(1L, v);
        }
    }

    @Test
    void appendLong_appendsCorrectly() {
        long[] arr = {1L, 2L, 3L};
        long[] result = OnnxModelLoader.appendLong(arr, 4L);
        assertEquals(4, result.length);
        assertEquals(4L, result[3]);
        assertEquals(3, arr.length); // original unchanged
    }

    @Test
    void loadGenerationConfig_returnsDefaults_ifFileNotFound() {
        OnnxModelLoader loader = new OnnxModelLoader(config());
        GenerationConfig cfg = loader.loadGenerationConfig(null);
        assertEquals(2, cfg.resolvedEosTokenId());
        assertEquals(512, cfg.resolvedMaxNewTokens(512));
    }

    @Test
    void close_closesSessionAndEnv() throws Exception {
        OnnxModelLoader loader = new OnnxModelLoader(config());
        OrtSession session = mock(OrtSession.class);
        OrtEnvironment env = mock(OrtEnvironment.class);
        setField(loader, "session", session);
        setField(loader, "env", env);

        loader.close();

        verify(session).close();
        verify(env).close();
    }

    // ---- helpers ----

    private static OrtException newOrtException() {
        return new OrtException("simulated load failure");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = OnnxModelLoader.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
