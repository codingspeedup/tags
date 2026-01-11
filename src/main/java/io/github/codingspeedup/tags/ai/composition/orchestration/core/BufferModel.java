package io.github.codingspeedup.tags.ai.composition.orchestration.core;

import io.github.codingspeedup.tags.ai.composition.orchestration.buffers.ChatMdModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.buffers.SourceCodeModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.buffers.TextModel;
import io.github.codingspeedup.tags.prompting.chat.PromptUtl;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.codingspeedup.tags.integration.groovy.ToolboxManagerService.GROOVY_EXTENSION;
import static io.github.codingspeedup.tags.prompting.chat.ChatMdUtl.CHAT_MD_EXTENSION;

@Getter
public abstract class BufferModel {

    public static final String PROMPT_REF_PREFIX = "@";

    public static final String ARG_SEPARATOR = "=";
    public static final String ARG_PLACEHOLDER = "∅";

    public static final String SECTION_NAME_START = "<";
    public static final String SECTION_NAME_END = ">";
    public static final String SECTION_CLOSE = "/";

    public static final String SECTION_ROOT_ID = "tags+";

    public static final String SECTION_REF_MARKER = "#";

    public static final String FILE_REF_MARKER = "file://";
    public static final String LINES_REF_MARKER = ":";

    private static final Map<String, BufferModel> MODEL_REGISTRY = new ConcurrentHashMap<>();

    private final String tPrefix;
    private final String aPrefix;
    private final String gPrefix;
    private final String sPrefix;
    private final String plusPrefix;

    protected BufferModel(String tPrefix, String aPrefix, String gPrefix, String sPrefix, String plusPrefix) {
        this.tPrefix = tPrefix;
        this.aPrefix = aPrefix;
        this.gPrefix = gPrefix;
        this.sPrefix = sPrefix;
        this.plusPrefix = plusPrefix;
    }

    public static Optional<BufferModel> of(String fileName) {
        fileName = fileName.toLowerCase(Locale.ROOT);
        var fileExtension = "." + StringUtils.trimToEmpty(FilenameUtils.getExtension(fileName));
        if (fileName.endsWith(CHAT_MD_EXTENSION)) {
            fileExtension = CHAT_MD_EXTENSION;
        }

        var fileModel = MODEL_REGISTRY.computeIfAbsent(fileExtension, (key) -> switch (key) {
            case ".java", ".go", GROOVY_EXTENSION, ".cs" -> new SourceCodeModel("// ") {
            };
            case ".txt" -> new TextModel() {
            };
            case CHAT_MD_EXTENSION -> new ChatMdModel() {
            };
            default -> null;
        });
        return Optional.ofNullable(fileModel);
    }

    public List<TAGPlusModel> locateTemplates(String content) {
        var templates = new ArrayList<TAGPlusModel>();

        var currentOffset = content.indexOf(tPrefix);
        while (currentOffset >= 0) {
            var bolOffset = indexOfBol(content, currentOffset);
            var eolOffset = indexOfEol(content, currentOffset);
            if (StringUtils.isBlank(content.substring(bolOffset, currentOffset))) {
                if (!templates.isEmpty()) {
                    templates.get(templates.size() - 1).setToOffset(currentOffset);
                }

                var block = new TAGPlusModel();
                block.setFromOffset(currentOffset);
                block.setToOffset(content.length());
                block.setTemplate(content.substring(currentOffset + tPrefix.length(), eolOffset));
                templates.add(block);
            }
            currentOffset = content.indexOf(tPrefix, eolOffset);
        }

        return templates;
    }

    public void fillTemplate(TAGPlusModel template, String content) {
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

    private SectionMarker parseSectionLine(String line) {
        if (line.startsWith(SECTION_NAME_START) && line.endsWith(SECTION_NAME_END)) {
            line = line.substring(SECTION_NAME_START.length(), line.length() - SECTION_NAME_END.length());
            var closing = line.startsWith(SECTION_CLOSE);
            if (closing) {
                line = StringUtils.trimToEmpty(line.substring(SECTION_CLOSE.length()));
            }
            return new SectionMarker(line, closing);
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
