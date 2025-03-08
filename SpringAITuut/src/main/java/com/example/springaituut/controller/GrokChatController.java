package com.example.springaituut.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
@RestController
@RequestMapping("/grok-chat")
public class GrokChatController {
    private final ChatClient chatClient;
private final List<Message> conversationHistory = new ArrayList<>();
    public GrokChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chats")
    public String chat(@RequestParam("message") String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Error: Message cannot be empty.";
        }
        String cleanedMessage = message.trim();
        conversationHistory.add((Message) new UserMessage(cleanedMessage));

        try {
            String response = chatClient.prompt().user(cleanedMessage).call().content();
            conversationHistory.add((Message) new UserMessage(response));
            return response;
        }
        catch (Exception e) {
            conversationHistory.add((Message) new UserMessage("Error: " + e.getMessage()));
            return "Error: " + e.getMessage();
        }

    }
    //Build an API that summarizes long texts/articles
    @GetMapping("/summarize")
    public String summarize(@RequestParam("text") String text) {
        // Input validation
        if (text == null || text.trim().isEmpty()) {
            return "Error: Text cannot be empty.";
        }

        String cleanedText = text.trim();
        // Using Spring AI's UserMessage
        UserMessage userMessage = new UserMessage(cleanedText);
        conversationHistory.add((Message) userMessage);

        try {
            // Prepare prompt for summarization
            String prompt = "Please provide a concise summary of the following text:\n" + cleanedText;

            // Call the chat client
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Using Spring AI's AssistantMessage
            AssistantMessage assistantMessage = new AssistantMessage(response);
            conversationHistory.add((Message) assistantMessage);
            return response;
        } catch (Exception e) {
            String errorMessage = "Error processing summary: " + e.getMessage();
            AssistantMessage errorResponse = new AssistantMessage(errorMessage);
            conversationHistory.add((Message) errorResponse);
            return errorMessage;
        }
    }

    @GetMapping("/summarize-url")
    public String summarizeUrl(@RequestParam("url") String url) throws MalformedURLException {
        // Input validation
        String content = fetchUrlContent(url);
        if (url == null || url.trim().isEmpty()) {
            return "Error: URL cannot be empty.";
        }

       UserMessage userMessage = new UserMessage(content);
        conversationHistory.add((Message) userMessage);

        try {
            // Prepare prompt for summarization
            String prompt = "Please provide a concise summary of the following URL:\n" + url;

            // Call the chat client
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Using Spring AI's AssistantMessage
            AssistantMessage assistantMessage = new AssistantMessage(response);
            conversationHistory.add((Message) assistantMessage);
            return response;
        } catch (Exception e) {
            String errorMessage = "Error processing summary: " + e.getMessage();
            AssistantMessage errorResponse = new AssistantMessage(errorMessage);
            conversationHistory.add((Message) errorResponse);
            return errorMessage;
        }
    }

    private String fetchUrlContent(String urlStr) throws MalformedURLException {
        URL url = new URL(urlStr);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString().trim();
    }
}
