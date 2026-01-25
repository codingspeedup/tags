package io.github.codingspeedup.tags.ai.composition_orchestration.buffers;

import io.github.codingspeedup.tags.ai.composition_orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.SectionModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.core.TagPlusModel;
import io.github.codingspeedup.tags.ai.primitives_reactive.PromptUtl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.codingspeedup.tags.minions.Minions.endsWith;

public class TextModel extends BufferModel {

    public static final String TEXT_EXTENSION = ".txt";

    public TextModel() {
        this(StringUtils.EMPTY);
    }

    protected TextModel(String lineCommentPrefix) {
        super(
                lineCommentPrefix + T_MARKER,
                lineCommentPrefix + A_MARKER,
                lineCommentPrefix + G_MARKER,
                lineCommentPrefix + S_MARKER,
                lineCommentPrefix + PLUS_MARKER
        );
    }

    public List<TagPlusModel> locateTagPlusRanges(String content) {
        var ranges = new ArrayList<TagPlusModel>();

        var currentOffset = content.indexOf(tPrefix);
        while (currentOffset >= 0) {
            var bolOffset = indexOfBol(content, currentOffset);
            var eolOffset = indexOfEol(content, currentOffset);
            if (StringUtils.isBlank(content.substring(bolOffset, currentOffset))) {
                if (!ranges.isEmpty()) {
                    ranges.get(ranges.size() - 1).setToOffset(currentOffset);
                }

                var block = new TagPlusModel();
                block.setFromOffset(currentOffset);
                block.setToOffset(content.length());
                block.setTemplate(content.substring(currentOffset + tPrefix.length(), eolOffset));
                ranges.add(block);
            }
            currentOffset = content.indexOf(tPrefix, eolOffset);
        }

        return ranges;
    }

    public void fillTagPlusModel(TagPlusModel tagPlus, String content) {
        var arguments = new StringBuilder();
        var plus = new StringBuilder();
        content = content.substring(tagPlus.getFromOffset(), tagPlus.getToOffset()).trim();
        content.lines().forEach(line -> {
            line = line.trim();
            if (line.startsWith(aPrefix)) {
                line = line.substring(aPrefix.length()).trim();
                if (!line.isEmpty()) {
                    arguments.append(line).append("\n");
                }
            } else if (line.startsWith(gPrefix)) {
                line = line.substring(gPrefix.length()).trim();
                tagPlus.setGateway(line);
            } else if (line.startsWith(plusPrefix)) {
                line = line.substring(plusPrefix.length()).trim();
                if (!line.isEmpty()) {
                    plus.append(line).append("\n");
                }
            }
        });
        tagPlus.setArguments(PromptUtl.parseProperties(arguments.toString()));
        tagPlus.setPlus(plus.toString().trim());
    }


    public Map<String, SectionModel> getSections(String content) {
        var sections = new HashMap<String, SectionModel>();

        var currentOffset = content.indexOf(sPrefix);
        while (currentOffset >= 0) {
            var bolOffset = indexOfBol(content, currentOffset);
            var eolOffset = indexOfEol(content, currentOffset);
            if (StringUtils.isBlank(content.substring(bolOffset, currentOffset))) {
                var line = StringUtils.trimToEmpty(content.substring(currentOffset + sPrefix.length(), eolOffset));

                var sModel = parseSectionLine(line);
                if (sModel != null) {
                    var name = sModel.name();
                    var block = sections.get(name);
                    if (sModel.closing()) {
                        if (block != null) {
                            block.setToOffset(eolOffset);
                        }
                    } else {
                        if (block != null) {
                            throw new UnsupportedOperationException("Duplicate section block `" + name + "'");
                        }

                        block = new SectionModel();
                        block.setName(name);
                        block.setFromOffset(currentOffset);
                        block.setToOffset(content.length());
                        sections.put(block.getName(), block);
                    }
                }
            }
            currentOffset = content.indexOf(sPrefix, eolOffset);
        }

        return sections;
    }

    @Override
    public Optional<String> stripTags(String content) {
        var contentChanged = new AtomicBoolean();
        var newContent = new StringBuilder();
        content.lines().forEach(line -> {
            var foo = line.stripLeading();
            if (foo.startsWith(tPrefix)
                    || foo.startsWith(aPrefix)
                    || foo.startsWith(gPrefix)
                    || foo.startsWith(sPrefix)
                    || foo.startsWith(plusPrefix)) {
                contentChanged.set(true);
            } else {
                newContent.append(line).append("\n");
            }
        });
        return contentChanged.get() ? Optional.of(newContent.toString()) : Optional.empty();
    }

    @Override
    public Triple<String, Integer, Integer> insertSection(String sectionName, String content, int fromOffset, int toOffset) {
        fromOffset = indexOfBol(content, fromOffset);
        toOffset = indexOfEol(content, toOffset);

        var newContent = new StringBuilder(content.length() + 64);
        newContent.append(content, 0, fromOffset);
        newContent.append(sPrefix).append(buildSectionStartMarker(sectionName)).append("\n");

        var startOffset = newContent.length();
        newContent.append(content, fromOffset, toOffset);

        var endOffset = newContent.length();
        if (!endsWith(newContent, "\n")) {
            newContent.append("\n");
        }
        newContent.append(sPrefix).append(buildSectionEndMarker(sectionName));
        if (toOffset < content.length() && content.charAt(toOffset) != '\n') {
            newContent.append("\n");
        }
        newContent.append(content, toOffset, content.length());

        return Triple.of(newContent.toString(), startOffset, endOffset);
    }


}
