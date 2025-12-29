package io.github.codingspeedup.tags.engine.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptUtl {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getDefaultSystemMessage() {
        return """
                Act as a senior engineer providing high-density, technically accurate info without fluff or polite filler.
                Prioritize immediate Markdown code blocks and use minimal prose only for non-obvious logic.
                """;
    }

    public static String getCurrentTimeIso() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String getSystemBlock(String message) {
        return String.format("""
                #### ⚙️ Intention
                ```system
                %s
                ```
                """, StringUtils.trimToEmpty(message));
    }

    public static String getUserBlock(String message) {
        return String.format("""
                ### 👤 User
                ```user
                %s
                ```
                """, StringUtils.trimToEmpty(message));
    }

    public static String getAssistantBlock(String message) {
        return String.format("""
                ### 🤖 Assistant - %s
                
                ---
                %s
                """, getCurrentTimeIso(), message);
    }


}
