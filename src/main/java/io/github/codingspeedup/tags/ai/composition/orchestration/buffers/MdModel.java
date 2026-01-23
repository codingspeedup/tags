package io.github.codingspeedup.tags.ai.composition.orchestration.buffers;

import io.github.codingspeedup.tags.ai.composition.orchestration.core.TagPlusModel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class MdModel extends MdModelBase {

    public static final String MD_EXTENSION = ".md";

    public MdModel() {
        super(MD_COMMENT_PREFIX + T_MARKER,
                MD_COMMENT_PREFIX + A_MARKER,
                MD_COMMENT_PREFIX + G_MARKER,
                MD_COMMENT_PREFIX + S_MARKER,
                MD_COMMENT_PREFIX + PLUS_MARKER);
    }

    @Override
    public List<TagPlusModel> locateTagPlusRanges(String content) {
        return List.of();
    }

    @Override
    public void fillTagPlusModel(TagPlusModel tagPlus, String content) {
    }

    @Override
    public Optional<String> stripTags(String content) {
        var contentChanged = new AtomicBoolean();
        var newContent = new StringBuilder();
        content.lines().forEach(line -> {
            var foo = line.strip();
            if ((foo.startsWith(tPrefix) && foo.endsWith(MD_COMMENT_SUFFIX))
                    || (foo.startsWith(aPrefix) && foo.endsWith(MD_COMMENT_SUFFIX))
                    || (foo.startsWith(gPrefix) && foo.endsWith(MD_COMMENT_SUFFIX))
                    || (foo.startsWith(sPrefix) && foo.endsWith(MD_COMMENT_SUFFIX))
                    || (foo.startsWith(plusPrefix) && foo.endsWith(MD_COMMENT_SUFFIX))) {
                contentChanged.set(true);
            } else {
                newContent.append(line).append("\n");
            }
        });
        return contentChanged.get() ? Optional.of(newContent.toString()) : Optional.empty();
    }

}
