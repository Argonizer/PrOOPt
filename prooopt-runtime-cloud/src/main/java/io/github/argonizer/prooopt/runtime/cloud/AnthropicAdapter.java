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
 * Calls Anthropic's Messages API ({@code POST /v1/messages}) for the CLOUD tiers. The text of the
 * response's content blocks is concatenated and returned for autoboxing.
 */
public class AnthropicAdapter extends AbstractHttpCloudAdapter {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    @Override
    public String generate(ModelConfig config, String prompt) {
        String apiKey = resolveApiKey(config, "PROOOPT_ANTHROPIC_API_KEY");

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelId());
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());
        body.put("messages", List.of(message));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", API_VERSION);
        headers.put("content-type", "application/json");

        JsonNode response = postJson(ENDPOINT, headers, body, config.getTimeoutMs());
        JsonNode content = response.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new PrOOPtException("Anthropic response contained no content blocks: " + response);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }
}
