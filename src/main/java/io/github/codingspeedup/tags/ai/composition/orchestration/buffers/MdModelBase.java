package io.github.codingspeedup.tags.ai.composition.orchestration.buffers;

import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.SectionModel;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.Map;

import static io.github.codingspeedup.tags.minions.Minions.endsWith;

public abstract class MdModelBase extends BufferModel {

    protected static final String MD_COMMENT_PREFIX = "[//]: # (";
    protected static final String MD_COMMENT_SUFFIX = ")";

    protected MdModelBase(String tPrefix, String aPrefix, String gPrefix, String sPrefix, String plusPrefix) {
        super(tPrefix, aPrefix, gPrefix, sPrefix, plusPrefix);
    }

    @Override
    public Map<String, SectionModel> getSections(String content) {
        var sections = new HashMap<String, SectionModel>();

        var currentOffset = content.indexOf(sPrefix);
        while (currentOffset >= 0) {
            var bolOffset = indexOfBol(content, currentOffset);
            if (bolOffset == currentOffset) {
                var eolOffset = indexOfEol(content, currentOffset);
                var line = content.substring(currentOffset, eolOffset).trim();
                if (line.endsWith(MD_COMMENT_SUFFIX)) {
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
            }
            currentOffset = content.indexOf(sPrefix, currentOffset + 1);
        }

        return sections;
    }

    @Override
    public Triple<String, Integer, Integer> insertSection(String sectionName, String content, int fromOffset, int toOffset) {
        fromOffset = indexOfBol(content, fromOffset);
        toOffset = indexOfEol(content, toOffset);

        var newContent = new StringBuilder(content.length() + 64);
        newContent.append(content, 0, fromOffset);
        while (!endsWith(newContent, "\n\n")) {
            newContent.append("\n");
        }
        newContent.append(sPrefix).append(buildSectionStartMarker(sectionName)).append(MD_COMMENT_SUFFIX);
        newContent.append("\n\n");

        var startOffset = newContent.length();
        newContent.append(content, fromOffset, toOffset);

        var endOffset = newContent.length();
        while (!endsWith(newContent, "\n\n")) {
            newContent.append("\n");
        }
        newContent.append(sPrefix).append(buildSectionEndMarker(sectionName)).append(MD_COMMENT_SUFFIX);
        if (toOffset < content.length()) {
            newContent.append("\n\n");
        }
        newContent.append(content, toOffset, content.length());

        return Triple.of(newContent.toString(), startOffset, endOffset);
    }

}
