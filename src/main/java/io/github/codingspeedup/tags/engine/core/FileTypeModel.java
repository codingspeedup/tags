package io.github.codingspeedup.tags.engine.core;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public abstract class FileTypeModel {

    private static final Map<String, FileTypeModel> MODEL_REGISTRY = new ConcurrentHashMap<>();

    private final String lineCommentPrefix;
    private final String tPrefix;
    private final String aPrefix;
    private final String gPrefix;
    private final String sPrefix;
    private final String plusPrefix;

    protected FileTypeModel(String lineCommentPrefix) {
        this.lineCommentPrefix = lineCommentPrefix;
        this.tPrefix = this.lineCommentPrefix + "T: ";
        this.aPrefix = this.lineCommentPrefix + "A: ";
        this.gPrefix = this.lineCommentPrefix + "G: ";
        this.sPrefix = this.lineCommentPrefix + "S: ";
        this.plusPrefix = this.lineCommentPrefix + "+: ";
    }

    private record S(String name, boolean closing) {
    }

    public static Optional<FileTypeModel> of(String fileName) {
        var fileExtension = StringUtils.trimToEmpty(FilenameUtils.getExtension(fileName)).toLowerCase(Locale.ROOT);
        var fileModel = MODEL_REGISTRY.computeIfAbsent(fileExtension, (key) -> switch (key) {
            case "java", "cs" -> new FileTypeModel("// ") {
            };
            default -> null;
        });
        return Optional.ofNullable(fileModel);
    }

    public List<TemplateBlock> locateTemplates(String content) {
        var templates = new ArrayList<TemplateBlock>();

        var currentOffset = content.indexOf(tPrefix);
        while (currentOffset >= 0) {
            var bolOffset = indexOfBol(content, currentOffset);
            var eolOffset = indexOfEol(content, currentOffset);
            if (StringUtils.isBlank(content.substring(bolOffset, currentOffset))) {
                if (!templates.isEmpty()) {
                    templates.get(templates.size() - 1).setToOffset(currentOffset);
                }

                var block = new TemplateBlock();
                block.setFromOffset(currentOffset);
                block.setToOffset(content.length());
                block.setTemplate(content.substring(currentOffset + tPrefix.length(), eolOffset));
                templates.add(block);
            }
            currentOffset = content.indexOf(sPrefix, eolOffset);
        }

        return templates;
    }

    public Map<String, SectionBlock> parseSections(String content) {
        var sections = new HashMap<String, SectionBlock>();

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

                        block = new SectionBlock();
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

    private S parseSectionLine(String line) {
        if (line.startsWith("<") && line.endsWith(">")) {
            line = line.substring(1, line.length() - 1);
            var closing = line.startsWith("/");
            if (closing) {
                line = StringUtils.trimToEmpty(line.substring(1));
            }
            return new S(line, closing);
        }
        return null;
    }


    public static int indexOfEol(String content, int startIndex) {
        int eol = content.indexOf('\n', startIndex);
        return (eol == -1) ? content.length() : eol;
    }

    public static int indexOfBol(String content, int endIndex) {
        int eol = content.lastIndexOf('\n', endIndex - 1);
        return (eol == -1) ? 0 : eol + 1;
    }

}
