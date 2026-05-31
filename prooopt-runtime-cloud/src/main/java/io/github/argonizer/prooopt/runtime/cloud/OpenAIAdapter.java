/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.runtime.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.argonizer.prooopt.config.ModelConfig;
import io.github.argonizer.prooopt.exception.PrOOPtException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls OpenAI's Chat Completions API ({@code POST /v1/chat/completions}) for the CLOUD tiers.
 */
public class OpenAIAdapter extends AbstractHttpCloudAdapter {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    @Override
    public String generate(ModelConfig config, String prompt) {
        String apiKey = resolveApiKey(config, "PROOOPT_OPENAI_API_KEY");

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelId());
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());
        body.put("messages", List.of(message));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("content-type", "application/json");

        JsonNode response = postJson(ENDPOINT, headers, body, config.getTimeoutMs());
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new PrOOPtException("OpenAI response contained no choices: " + response);
        }
        return choices.get(0).path("message").path("content").asText();
    }
}
