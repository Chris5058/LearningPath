
package com.example.springaituut;

        import org.springframework.ai.chat.client.ChatClient;
        import org.springframework.ai.chat.model.ChatModel;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${grok.api.key}")
    private String apiKey;

    @Bean
    public ChatModel chatModel() {
        return new GrokChatModel(apiKey);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}