package io.github.codingspeedup.tags.utils;

import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static io.github.codingspeedup.tags.utils.TagsUtl.getOpenTabNames;

public class ChatUtl {

    public static final String CHAT_MD_EXTENSION = ".chat.md";
    private static final String CHAT_MD_VERSION = "-v";

    public static final String PARAMETERS_BLOCK_INFO = "llm-parameters";
    public static final String SYSTEM_BLOCK_INFO = "llm-system-message";
    public static final String USER_BLOCK_INFO = "llm-user-message";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public static String nextBufferName(Project project, String fileName) {
        var vPos = fileName.lastIndexOf(CHAT_MD_VERSION);

        var bufferPrefix = vPos < 0
                ? fileName + CHAT_MD_VERSION
                : fileName.substring(0, vPos + CHAT_MD_VERSION.length());

        var previousVersions = getOpenTabNames(project).stream()
                .filter(name -> name.endsWith(CHAT_MD_EXTENSION) && name.startsWith(bufferPrefix))
                .map(name -> name.substring(bufferPrefix.length(), name.length() - CHAT_MD_EXTENSION.length()))
                .toList();

        var versionString = StringUtils.EMPTY;
        if (vPos >= 0) {
            versionString = fileName.substring(bufferPrefix.length(), fileName.length() - CHAT_MD_EXTENSION.length());
        }
        var versionLevel = vPos < 0 ? 0 : StringUtils.countMatches(versionString, ".") + 1;

        var nextVersion = previousVersions.stream()
                .filter(name -> StringUtils.countMatches(name, ".") == versionLevel)
                .mapToInt(version -> {
                    var lastDotIdx = version.lastIndexOf('.');
                    if (lastDotIdx >= 0) {
                        version = version.substring(lastDotIdx + 1);
                    }
                    try {
                        return Integer.parseInt(version);
                    } catch (Exception ex) {
                        return 0;
                    }
                })
                .max()
                .orElse(0) + 1;

        if (versionString.isEmpty()) {
            versionString = String.valueOf(nextVersion);
        } else {
            versionString = versionString + "." + nextVersion;
        }

        return bufferPrefix + versionString + CHAT_MD_EXTENSION;
    }

    public static String renderParametersBlock(Properties parameters) {
        var writer = new StringBuilder();
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                        if (value != null) {
                            writer.append(key).append("=").append(value).append("\n");
                        }
                    }
            );
        }
        if (!writer.isEmpty()) {
            writer.setLength(writer.length() - 1);
        }
        return String.format("""
                #### 🛠️ parameters
                ```%s
                %s
                ```
                """, PARAMETERS_BLOCK_INFO, writer);
    }

    public static String renderSystemBlock(String message) {
        return String.format("""
                #### ⚙️ Intention
                ```%s
                %s
                ```
                """, SYSTEM_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderUserBlock(String message) {
        return String.format("""
                ### 👤 User
                ```%s
                %s
                ```
                """, USER_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderAiBlock(String message) {
        var messageTimestamp = LocalDateTime.now().format(FORMATTER);
        return String.format("""
                
                #### 🤖 AI: %s
                
                ---
                %s
                """, messageTimestamp, message);
    }

    public static String renderSystemBlock(SystemMessage message) {
        return renderSystemBlock(message.text());
    }

    public static String renderUserBlock(UserMessage message) {
        return renderUserBlock(message.singleText());
    }
}
