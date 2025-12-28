package io.github.codingspeedup.tags.integration;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class GeminiIntegration {

    public static ChatRequest.Builder getDefaultRequestBuilder() {
        return ChatRequest.builder()
                .modelName("gemini-flash-latest")
                .temperature(0.7)
                .maxOutputTokens(1000);
    }

    public static void main(String[] args) {
        // Ideally, load this from an environment variable: System.getenv("GOOGLE_AI_GEMINI_API_KEY")
        String apiKey = "";

        // Initialize the Gemini Chat Model
        var model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
//                .modelName("gemini-flash-latest")
//                .temperature(0.7) // Controls randomness (0.0 to 1.0)
//                .maxOutputTokens(1000)
                .build();

        // Define a prompt
        String userPrompt = "Explain the concept of dependency injection in Java in two sentences.";

        try {

            var chatRequest = getDefaultRequestBuilder().messages(UserMessage.from(userPrompt)).build();
            // Generate response
            var chatResponse = model.chat(chatRequest);

            System.out.println("User: " + userPrompt);
            System.out.println("Gemini: " + chatResponse.aiMessage().text());
        } catch (Exception e) {
            System.err.println("Error communicating with Gemini API: " + e.getMessage());
        }
    }

}
