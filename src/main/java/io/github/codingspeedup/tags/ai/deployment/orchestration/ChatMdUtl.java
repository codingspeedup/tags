package io.github.codingspeedup.tags.ai.deployment.orchestration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.ai.composition.orchestration.buffers.ChatMdModel;
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

    private static final String CHAT_MD_VERSION = "-v";

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
        return String.format("Chat%s%d%s", CHAT_MD_VERSION, version, ChatMdModel.CHAT_MD_EXTENSION);
    }

    public static String nextChatMdBufferName(Project project, String fileName) {
        var vPos = fileName.lastIndexOf(CHAT_MD_VERSION);

        var bufferPrefix = vPos < 0
                ? fileName + CHAT_MD_VERSION
                : fileName.substring(0, vPos + CHAT_MD_VERSION.length());

        var previousVersions = getOpenTabNames(project).stream()
                .filter(name -> name.endsWith(ChatMdModel.CHAT_MD_EXTENSION) && name.startsWith(bufferPrefix))
                .map(name -> name.substring(bufferPrefix.length(), name.length() - ChatMdModel.CHAT_MD_EXTENSION.length()))
                .toList();

        var versionString = StringUtils.EMPTY;
        if (vPos >= 0) {
            versionString = fileName.substring(bufferPrefix.length(), fileName.length() - ChatMdModel.CHAT_MD_EXTENSION.length());
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

        return bufferPrefix + versionString + ChatMdModel.CHAT_MD_EXTENSION;
    }

    public static String renderParametersBlock(Properties parameters) {
        var content = new StringBuilder();
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                        if (value != null) {
                            content.append(key).append("=").append(value).append("\n");
                        }
                    }
            );
        }
        if (!content.isEmpty()) {
            content.setLength(content.length() - 1);
            content.insert(0, "\n");
        }
        return String.format("""
                #### 🛠️ parameters
                ```%s%s
                ```
                """, ChatMdModel.PARAMETERS_BLOCK_INFO, content);
    }

    public static String renderSystemBlock(String message) {
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isNotEmpty(message)) {
            message = "\n" + message;
        }
        return String.format("""
                #### 📜 Guidelines
                ```%s%s
                ```
                """, ChatMdModel.SYSTEM_BLOCK_INFO, message);
    }

    public static String renderUserBlock(String message) {
        return String.format("""
                ### 👤 User
                `````%s
                %s
                `````
                """, ChatMdModel.USER_BLOCK_INFO, StringUtils.trimToEmpty(message));
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
                """, agent, messageTimestamp, StringUtils.trimToEmpty(message));
    }

    public static String renderSystemBlock(SystemMessage message) {
        return renderSystemBlock(message.text());
    }

    public static String renderUserBlock(UserMessage message) {
        return renderUserBlock(message.singleText());
    }

}
