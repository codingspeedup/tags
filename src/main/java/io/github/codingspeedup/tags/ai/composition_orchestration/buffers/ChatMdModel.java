package io.github.codingspeedup.tags.ai.composition_orchestration.buffers;

import com.intellij.openapi.project.Project;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.TagPlusModel;
import io.github.codingspeedup.tags.ai.primitives_reactive.PromptUtl;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.codingspeedup.tags.minions.PluginUtl.getOpenTabNames;

public class ChatMdModel extends MdModelBase {

    public static final String CHAT_MD_EXTENSION = ".chat.md";

    public static final String PARAMETERS_BLOCK_INFO = "llm-parameters";
    public static final String SYSTEM_BLOCK_INFO = "llm-system-message";
    public static final String USER_BLOCK_INFO = "llm-user-message";

    private static final String CHAT_MD_VERSION = "-v";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    public ChatMdModel() {
        super(StringUtils.EMPTY,
                A_MARKER,
                StringUtils.EMPTY,
                MD_COMMENT_PREFIX + S_MARKER,
                PLUS_MARKER);
    }


    public static String buildChatMdFileName(int version) {
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
                """, PARAMETERS_BLOCK_INFO, content);
    }

    public static String renderSystemBlock(String message) {
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isNotEmpty(message)) {
            message = "\n" + message;
        }
        return String.format("""
                #### 📜 Guidelines
                `````%s%s
                `````
                """, SYSTEM_BLOCK_INFO, message);
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
                """, agent, messageTimestamp, StringUtils.trimToEmpty(message));
    }

    public static String renderSystemBlock(SystemMessage message) {
        return renderSystemBlock(message.text());
    }

    public static String renderUserBlock(UserMessage message) {
        return renderUserBlock(message.singleText());
    }

    protected static TagPlusModel toTagPlusRange(FencedCodeBlock codeBlock) {
        var tagPlusModel = new TagPlusModel();
        tagPlusModel.setFromOffset(codeBlock.getInfo().getEndOffset());
        tagPlusModel.setToOffset(codeBlock.getClosingMarker().getStartOffset());
        return tagPlusModel;
    }

    @Override
    public List<TagPlusModel> locateTagPlusRanges(String content) {
        return List.of();
    }

    @Override
    public void fillTagPlusModel(TagPlusModel tagPlus, String content) {
        content = content.substring(tagPlus.getFromOffset(), tagPlus.getToOffset()).trim();
        var template = new StringBuilder();
        var arguments = new StringBuilder();
        var plus = new StringBuilder();
        content.lines().forEach(line -> {
            line = line.trim();
            if (line.startsWith(aPrefix)) {
                line = line.substring(aPrefix.length()).trim();
                if (!line.isEmpty()) {
                    arguments.append(line).append("\n");
                }
            } else if (line.startsWith(plusPrefix)) {
                line = line.substring(plusPrefix.length()).trim();
                if (!line.isEmpty()) {
                    plus.append(line).append("\n");
                }
            } else {
                template.append(line.trim()).append("\n");
            }
        });
        tagPlus.setTemplate(template.toString().trim());
        tagPlus.setArguments(PromptUtl.parseProperties(arguments.toString()));
        tagPlus.setPlus(plus.toString().trim());
    }

    @Override
    public Optional<String> stripTags(String content) {
        var contentChanged = new AtomicBoolean();
        var newContent = new StringBuilder();
        content.lines().forEach(line -> {
            var foo = line.strip();
            if (foo.startsWith(sPrefix) && foo.endsWith(MD_COMMENT_SUFFIX)) {
                contentChanged.set(true);
            } else {
                newContent.append(line).append("\n");
            }
        });
        return contentChanged.get() ? Optional.of(newContent.toString()) : Optional.empty();
    }

}

