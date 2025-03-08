package com.example.springaituut.model;

import org.springframework.ai.chat.messages.AssistantMessage;
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

import java.util.List;
import java.util.Map;

public class GrokChatModel implements  ChatModel {
    private final String apiKey = "xai-h8HVxq4j6K7SFxZIQgwQQDTBEgfg2FTAecKf75lJGEbRJEAzyzpkHDt7vGm8fygdeyUTYaXN7BqIRmPU";
    private final String apiUrl = "https://api.x.ai/v1/chat/completions"; // Confirm this
    private final RestTemplate restTemplate = new RestTemplate();
    private final String model = "grok-2-latest"; // Confirm with xAI docs

    @Override
    public ChatResponse call(Prompt prompt) {
        try{
            List< Map<String, String> > instructions = (List<Map<String, String>>) prompt.getInstructions().stream()
                    .map(m ->Map.of("role", m instanceof UserMessage ? "user" : "assistant",
                            "content", m.toString()));
            // Build request body
            Map<String, Object> requestBody = Map.of("model", "grok-2-latest",
                    "messages", instructions);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Grok API");
            }

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
                throw new RuntimeException("No 'content' field in message");
            }


            AssistantMessage assistantMessage = new AssistantMessage(content);
            Generation generation = new Generation(assistantMessage);
            return new ChatResponse(List.of(generation));


        }
        catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
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
            public Integer getTopK() {
                return null;
            }

            @Override
            public Double getTopP() {
                return null;
            }

            @Override
            public <T extends ChatOptions> T copy() {
                return null;
            }
        };
    }
}
