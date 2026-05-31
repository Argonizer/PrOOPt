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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.argonizer.prooopt.config.CredentialResolver;
import io.github.argonizer.prooopt.config.ModelConfig;
import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.exception.PrOOPtException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Shared plumbing for HTTP-based cloud adapters: a reused {@link HttpClient}, JSON (de)serialization,
 * credential resolution, and consistent error reporting. Subclasses build the request body and pull
 * the text out of the response.
 */
public abstract class AbstractHttpCloudAdapter implements CloudAdapter {

    protected final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final CredentialResolver credentials = new CredentialResolver();

    /** Resolves the configured API key, failing fast with guidance when it is absent. */
    protected String resolveApiKey(ModelConfig config, String envHint) {
        String key = credentials.resolve(config.getApiKey());
        if (key == null || key.isBlank()) {
            throw new PrOOPtConfigException(
                    "no API key resolved for cloud model '" + config.getModelId() + "'. Set it in the "
                            + "environment (for example " + envHint + ") and reference it from YAML as "
                            + "${" + envHint + "}.");
        }
        return key;
    }

    /** Sends a JSON POST and returns the parsed response body, or throws on a non-2xx status. */
    protected JsonNode postJson(String url, Map<String, String> headers, Object body, long timeoutMs) {
        try {
            String payload = mapper.writeValueAsString(body);
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 60_000L))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            headers.forEach(request::header);

            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new PrOOPtException("cloud request to " + url + " failed with HTTP "
                        + response.statusCode() + ": " + truncate(response.body()));
            }
            return mapper.readTree(response.body());
        } catch (PrOOPtException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new PrOOPtException("cloud request to " + url + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrOOPtException("cloud request to " + url + " was interrupted", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }
}
