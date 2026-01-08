package io.github.codingspeedup.tags.prompting.tags;

import io.github.codingspeedup.tags.prompting.chat.PromptUtl;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public abstract class TemplateModel {

    public static final String TEMPLATE_PREFIX = "@";

    public static final String VAR_SEPARATOR = "=";
    public static final String VAR_PLACEHOLDER = "∅";

    public static final String SECTION_NAME_START = "<";
    public static final String SECTION_NAME_END = ">";
    public static final String SECTION_CLOSE = "/";

    public static final String SECTION_ROOT_ID = "tags+";

    public static final String SECTION_REF_MARKER = "#";
    public static final String FILE_REF_MARKER = "file://";
    public static final String LINES_REF_MARKER = ":";

    private static final Map<String, TemplateModel> MODEL_REGISTRY = new ConcurrentHashMap<>();

    private final String lineCommentPrefix;
    private final String lineCommentSuffix;
    private final String tPrefix;
    private final String aPrefix;
    private final String gPrefix;
    private final String sPrefix;
    private final String plusPrefix;

    protected TemplateModel(String lineCommentPrefix, String lineCommentSuffix) {
        this.lineCommentPrefix = lineCommentPrefix;
        this.lineCommentSuffix = lineCommentSuffix;
        this.tPrefix = this.lineCommentPrefix + "T: ";
        this.aPrefix = this.lineCommentPrefix + "A: ";
        this.gPrefix = this.lineCommentPrefix + "G: ";
        this.sPrefix = this.lineCommentPrefix + "S: ";
        this.plusPrefix = this.lineCommentPrefix + "+: ";
    }

    public record S(String name, boolean closing) {
    }

    public static Optional<TemplateModel> of(String fileName) {
        var fileExtension = StringUtils.trimToEmpty(FilenameUtils.getExtension(fileName)).toLowerCase(Locale.ROOT);
        var fileModel = MODEL_REGISTRY.computeIfAbsent(fileExtension, (key) -> switch (key) {
            case "txt" -> new TemplateModel(StringUtils.EMPTY, StringUtils.EMPTY) {
            };
            case "java", "cs" -> new TemplateModel("// ", StringUtils.EMPTY) {
            };
            default -> null;
        });
        return Optional.ofNullable(fileModel);
    }

    public List<TemplateBlock> identifyTemplates(String content) {
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
            currentOffset = content.indexOf(tPrefix, eolOffset);
        }

        return templates;
    }

    public void fillTemplate(TemplateBlock template, String content) {
        var arguments = new StringBuilder();
        var plus = new StringBuilder();
        content = content.substring(template.getFromOffset(), template.getToOffset());
        content.lines().forEach(line -> {
            line = line.trim();
            if (line.startsWith(aPrefix)) {
                line = line.substring(aPrefix.length()).trim();
                if (!line.isEmpty()) {
                    arguments.append(line).append("\n");
                }
            } else if (line.startsWith(gPrefix)) {
                line = line.substring(gPrefix.length()).trim();
                template.setGateway(line);
            } else if (line.startsWith(plusPrefix)) {
                line = line.substring(plusPrefix.length()).trim();
                if (!line.isEmpty()) {
                    plus.append(line).append("\n");
                }
            }
        });
        template.setArguments(PromptUtl.parseProperties(arguments.toString()));
        template.setPlus(plus.toString().trim());
    }


    public Map<String, SectionBlock> getSections(String content) {
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
        if (line.startsWith(SECTION_NAME_START) && line.endsWith(SECTION_NAME_END)) {
            line = line.substring(SECTION_NAME_START.length(), line.length() - SECTION_NAME_END.length());
            var closing = line.startsWith(SECTION_CLOSE);
            if (closing) {
                line = StringUtils.trimToEmpty(line.substring(SECTION_CLOSE.length()));
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
