package io.github.codingspeedup.tags.ai.composition_orchestration.core;

import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.ChatMdModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.MdModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.SourceCodeModel;
import io.github.codingspeedup.tags.ai.composition_orchestration.buffers.TextModel;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.codingspeedup.tags.ai.composition_orchestration.buffers.ChatMdModel.CHAT_MD_EXTENSION;
import static io.github.codingspeedup.tags.ai.composition_orchestration.buffers.MdModel.MD_EXTENSION;
import static io.github.codingspeedup.tags.ai.composition_orchestration.buffers.TextModel.TEXT_EXTENSION;
import static io.github.codingspeedup.tags.minions.ToolboxManagerService.GROOVY_EXTENSION;

@Getter
public abstract class BufferModel {

    protected static final String T_MARKER = "T: ";
    protected static final String A_MARKER = "A: ";
    protected static final String G_MARKER = "G: ";
    protected static final String S_MARKER = "S: ";
    protected static final String PLUS_MARKER = "+: ";

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
    public static final String LINES_REF_INTERVAL = "-";
    public static final String LINES_REF_GROUP_SEPARATOR = ",";

    private static final Map<String, BufferModel> MODEL_REGISTRY = new ConcurrentHashMap<>();

    protected final String tPrefix;
    protected final String aPrefix;
    protected final String gPrefix;
    protected final String sPrefix;
    protected final String plusPrefix;

    protected BufferModel(String tPrefix, String aPrefix, String gPrefix, String sPrefix, String plusPrefix) {
        this.tPrefix = tPrefix;
        this.aPrefix = aPrefix;
        this.gPrefix = gPrefix;
        this.sPrefix = sPrefix;
        this.plusPrefix = plusPrefix;
    }

    public static Optional<BufferModel> of(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        fileName = fileName.toLowerCase(Locale.ROOT);
        var fileExtension = "." + StringUtils.trimToEmpty(FilenameUtils.getExtension(fileName));
        if (fileName.endsWith(CHAT_MD_EXTENSION)) {
            fileExtension = CHAT_MD_EXTENSION;
        }

        var fileModel = MODEL_REGISTRY.computeIfAbsent(fileExtension, (key) -> switch (key) {
            case CHAT_MD_EXTENSION -> new ChatMdModel() {
            };
            case MD_EXTENSION -> new MdModel() {
            };
            case ".java", ".go", GROOVY_EXTENSION, ".cs" -> new SourceCodeModel("// ") {
            };
            case TEXT_EXTENSION -> new TextModel() {
            };
            default -> null;
        });
        return Optional.ofNullable(fileModel);
    }

    public static int indexOfEol(String content, int startIndex) {
        int eol = content.indexOf('\n', startIndex);
        return (eol == -1) ? content.length() : eol;
    }

    public static int indexOfBol(String content, int endIndex) {
        int eol = content.lastIndexOf('\n', endIndex - 1);
        return (eol == -1) ? 0 : eol + 1;
    }

    public static String parseSectionName(String value) {
        return value.substring(1).trim();
    }

    public static @NonNull String getNextSectionName(Set<String> existingSections) {
        var sectionIndex = 1;
        var newSectionName = SECTION_ROOT_ID + sectionIndex;
        while (existingSections.contains(newSectionName)) {
            newSectionName = SECTION_ROOT_ID + (++sectionIndex);
        }
        return newSectionName;
    }

    public static @NonNull String buildSectionStartMarker(String sectionName) {
        return SECTION_NAME_START + sectionName + SECTION_NAME_END;
    }

    public static @NonNull String buildSectionEndMarker(String sectionName) {
        return SECTION_NAME_START + SECTION_CLOSE + sectionName + SECTION_NAME_END;
    }

    public abstract List<TagPlusModel> locateTagPlusRanges(String content);

    public abstract void fillTagPlusModel(TagPlusModel tagPlus, String content);

    public abstract Map<String, SectionModel> getSections(String content);

    public abstract Optional<String> stripTags(String content);

    public abstract Triple<String, Integer, Integer> insertSection(String sectionName, String content, int fromOffset, int toOffset);

    protected static SectionMarker parseSectionLine(String line) {
        var fromIndex = line.indexOf(SECTION_NAME_START);
        if (fromIndex >= 0) {
            fromIndex += SECTION_NAME_START.length();
            var toIndex = line.indexOf(SECTION_NAME_END, fromIndex);
            line = line.substring(fromIndex, toIndex);
            var closing = line.startsWith(SECTION_CLOSE);
            if (closing) {
                line = StringUtils.trimToEmpty(line.substring(SECTION_CLOSE.length()));
            }
            return new SectionMarker(line, closing);
        }
        return null;
    }

}
