package com.example.springaituut;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrokChatModel implements ChatModel {
    private final String apiKey;
    private final String apiUrl = "https://api.x.ai/v1/chat/completions";
    private final RestTemplate restTemplate;
    private final String model = "grok-2-latest"; // Adjust based on actual Grok API model name

    public GrokChatModel(String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            String message = prompt.getInstructions().stream()
                    .filter(m -> m instanceof UserMessage)
                    .map(Message::toString)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No user message found in prompt"));

            // Build request body
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(userMessage));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Grok API");
            }

            // Debug: Print the full response body
            System.out.println("API Response: " + responseBody);

            // Attempt to parse response (assuming OpenAI-like structure)
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No 'choices' field in API response");
            }

            Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
            if (messageObj == null) {
                throw new RuntimeException("No 'message' field in choices");
            }

            String content = (String) messageObj.get("content");
            if (content == null) {
                // Fallback: Check for 'text' field if 'content' is missing
                content = (String) choices.get(0).get("text");
                if (content == null) {
                    throw new RuntimeException("No 'content' or 'text' field in API response");
                }
            }

            AssistantMessage assistantMessage = new AssistantMessage(content);
            Generation generation = new Generation(assistantMessage);
            return new ChatResponse(List.of(generation));

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Grok API: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return new ChatOptions() {
            @Override
            public String getModel() {
                return null;
            }

            @Override
            public Double getFrequencyPenalty() {
                return null;
            }

            @Override
            public Integer getMaxTokens() {
                return null;
            }

            @Override
            public Double getPresencePenalty() {
                return null;
            }

            @Override
            public List<String> getStopSequences() {
                return null;
            }

            @Override
            public Double getTemperature() {
                return null;
            }


            @Override
            public <T extends ChatOptions> T copy() {
                return null;
            }

            @Override
            public Integer getTopK() {
                return null;
            }

            @Override
            public Double getTopP() {
                return null;
            }
            // Add default options if needed
        };
    }
}