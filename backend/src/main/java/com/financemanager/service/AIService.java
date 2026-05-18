package com.financemanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final WebClient webClient;

    @Value("${ai.ollama.api-url}")
    private String ollamaApiUrl;

    @Value("${ai.ollama.model}")
    private String ollamaModel;

    public String categorizeTransaction(String description, String amount) {
        String prompt = "Categorize this transaction description into a single short category (e.g., Food, Transport, Rent, Salary, Utilities, Shopping, Entertainment, Health). Description: \""
                + description + "\", Amount: " + amount + ". Respond ONLY with the category name, nothing else.";
        return callOllama(prompt);
    }

    public String getFinancialAdvice(String userContext) {
        String prompt = "You are a helpful financial advisor. Based on this summary of transactions, give a brief financial tip: "
                + userContext;
        return callOllama(prompt);
    }

    private String callOllama(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            System.out.println("Calling Ollama at: " + ollamaApiUrl);
            System.out.println("Using model: " + ollamaModel);
            System.out.println("Prompt: " + prompt);

            OllamaResponse response = webClient.post()
                    .uri(ollamaApiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block();

            if (response != null && response.getResponse() != null) {
                String category = response.getResponse().trim();
                System.out.println("Ollama response: " + category);
                return category;
            }
        } catch (Exception e) {
            System.err.println("Error calling Ollama: " + e.getMessage());
            e.printStackTrace();
            return "Uncategorized"; // Fallback
        }
        return "Uncategorized";
    }

    // Helper class for Ollama response mapping
    // is the DTO or POJO for the response
    // mapped at the line : .bodyToMono(OllamaResponse.class)
    // here only the setResponse is called and response is set
    static class OllamaResponse {
        private String response;

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}
