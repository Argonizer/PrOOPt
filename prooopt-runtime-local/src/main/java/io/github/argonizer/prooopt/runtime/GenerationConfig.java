/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Sampling/stop parameters loaded from {@code generation_config.json} alongside the ONNX model.
 * When the file is absent, sensible defaults are used.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerationConfig {

    /** {@code int} or {@code List<Integer>} — the HuggingFace format varies between models. */
    @JsonProperty("eos_token_id")
    private Object eosTokenId;

    @JsonProperty("pad_token_id")
    private Integer padTokenId;

    @JsonProperty("bos_token_id")
    private Integer bosTokenId;

    @JsonProperty("max_new_tokens")
    private Integer maxNewTokens;

    public Object getEosTokenId() {
        return eosTokenId;
    }

    public void setEosTokenId(Object eosTokenId) {
        this.eosTokenId = eosTokenId;
    }

    public Integer getPadTokenId() {
        return padTokenId;
    }

    public void setPadTokenId(Integer padTokenId) {
        this.padTokenId = padTokenId;
    }

    public Integer getBosTokenId() {
        return bosTokenId;
    }

    public void setBosTokenId(Integer bosTokenId) {
        this.bosTokenId = bosTokenId;
    }

    public Integer getMaxNewTokens() {
        return maxNewTokens;
    }

    public void setMaxNewTokens(Integer maxNewTokens) {
        this.maxNewTokens = maxNewTokens;
    }

    /**
     * Resolves the EOS token ID as a single int. If {@code eosTokenId} is a list, the first element
     * is used. Defaults to {@code 2} (the standard EOS ID for most models).
     */
    public int resolvedEosTokenId() {
        if (eosTokenId instanceof Number n) {
            return n.intValue();
        }
        if (eosTokenId instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Number n) {
            return n.intValue();
        }
        return 2;
    }

    /** Returns {@code maxNewTokens}, or {@code defaultValue} when it was not specified. */
    public int resolvedMaxNewTokens(int defaultValue) {
        return maxNewTokens != null ? maxNewTokens : defaultValue;
    }
}
