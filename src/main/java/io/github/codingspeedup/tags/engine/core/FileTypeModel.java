package io.github.codingspeedup.tags.engine.core;

import io.github.codingspeedup.tags.engine.tags.SModel;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    public static Optional<FileTypeModel> of(String fileName) {
        var fileExtension = StringUtils.trimToEmpty(FilenameUtils.getExtension(fileName)).toLowerCase(Locale.ROOT);
        var fileModel = MODEL_REGISTRY.computeIfAbsent(fileExtension, (key) -> switch (key) {
            case "java", "cs" -> new FileTypeModel("// ") {
            };
            default -> null;
        });
        return Optional.ofNullable(fileModel);
    }

    public SModel s(String line) {
        line = StringUtils.trimToEmpty(line);
        if (line.startsWith("<") && line.endsWith(">")) {
            line = line.substring(1, line.length() - 1);
            var closing = line.startsWith("/");
            if (closing) {
                line =  StringUtils.trimToEmpty(line.substring(1));
            }
            return new SModel(line, closing);
        }
        return null;
    }

}
