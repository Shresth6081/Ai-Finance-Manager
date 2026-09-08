package com.financemanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OllamaHealthIndicator implements HealthIndicator {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Health.up()
                        .withDetail("url", ollamaBaseUrl)
                        .withDetail("status", "Ollama is running")
                        .build();
            } else {
                return Health.down()
                        .withDetail("url", ollamaBaseUrl)
                        .withDetail("statusCode", response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.toString();
            }
            return Health.down()
                    .withDetail("url", ollamaBaseUrl != null ? ollamaBaseUrl : "unknown")
                    .withDetail("error", errorMsg)
                    .build();
        }
    }

    public boolean isUp() {
        return health().getStatus().equals(Status.UP);
    }
}
