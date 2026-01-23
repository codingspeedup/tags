package io.github.codingspeedup.tags.ai.composition.orchestration.buffers;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.TagPlusModel;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptUtl;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatMdModel extends MdModelBase {

    public static final String CHAT_MD_EXTENSION = ".chat.md";

    public static final String PARAMETERS_BLOCK_INFO = "llm-parameters";
    public static final String SYSTEM_BLOCK_INFO = "llm-system-message";
    public static final String USER_BLOCK_INFO = "llm-user-message";

    public ChatMdModel() {
        super(StringUtils.EMPTY,
                A_MARKER,
                StringUtils.EMPTY,
                MD_COMMENT_PREFIX + S_MARKER,
                PLUS_MARKER);
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

