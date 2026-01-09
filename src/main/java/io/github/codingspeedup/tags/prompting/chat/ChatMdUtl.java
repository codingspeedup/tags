package io.github.codingspeedup.tags.prompting.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static io.github.codingspeedup.tags.plugin.core.TagsUtl.getOpenTabNames;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMdUtl {

    public static final String CHAT_MD_EXTENSION = ".chat.md";
    private static final String CHAT_MD_VERSION = "-v";

    public static final String PARAMETERS_BLOCK_INFO = "llm-parameters";
    public static final String SYSTEM_BLOCK_INFO = "llm-system-message";
    public static final String USER_BLOCK_INFO = "llm-user-message";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public static VirtualFile nextChatMdFile(VirtualFile chatFolder) throws IOException {
        var version = 1;
        var fileName = buildChatMdFileName(version);
        while (chatFolder.findChild(fileName) != null) {
            fileName = buildChatMdFileName(++version);
        }
        return chatFolder.createChildData(ChatMdUtl.class, fileName);
    }

    private static @NonNull String buildChatMdFileName(int version) {
        return String.format("Chat%s%d%s", CHAT_MD_VERSION, version, CHAT_MD_EXTENSION);
    }

    public static String nextChatMdBufferName(Project project, String fileName) {
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
                #### 📜 Guidelines
                ```%s
                %s
                ```
                """, SYSTEM_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderUserBlock(String message) {
        return String.format("""
                ### 👤 User
                `````%s
                %s
                `````
                """, USER_BLOCK_INFO, StringUtils.trimToEmpty(message));
    }

    public static String renderAiBlock(String message) {
        return renderResponseBlock("LLM", message);
    }

    public static String renderResponseBlock(String agent, String message) {
        var messageTimestamp = LocalDateTime.now().format(FORMATTER);
        return String.format("""
                
                #### 🤖 %s: %s
                
                ---
                %s
                """, agent, messageTimestamp, message);
    }

    public static String renderSystemBlock(SystemMessage message) {
        return renderSystemBlock(message.text());
    }

    public static String renderUserBlock(UserMessage message) {
        return renderUserBlock(message.singleText());
    }

    public static String sanitizeLineEndings(String message) {
        if (message == null) {
            return StringUtils.EMPTY;
        }
        return message.replaceAll("\\r\\n|\\r", "\n");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean endsWith(StringBuilder sb, String text) {
        if (sb == null) {
            return text == null;
        }
        if (text == null) {
            return false;
        }
        var textLen = text.length();
        if (textLen == 0) {
            return true;
        }
        var sbLen = sb.length();
        if (sbLen < textLen) {
            return false;
        }
        for (var i = 0; i < textLen; i++) {
            if (sb.charAt(sbLen - textLen + i) != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

}
