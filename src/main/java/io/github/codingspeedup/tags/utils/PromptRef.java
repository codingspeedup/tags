package io.github.codingspeedup.tags.utils;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class PromptRef {

    @Getter
    private final String id;
    private final String[] path;

    public PromptRef(String ref) {
        ref = StringUtils.trimToEmpty(ref);
        if (ref.startsWith("@")) {
            ref = ref.substring(1);
        }
        var chunks = Arrays.stream(ref.split("\\."))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotEmpty)
                .toList();
        this.id = ref.endsWith(".") ? StringUtils.EMPTY : chunks.get(chunks.size() - 1);
        this.path = (ref.endsWith(".") ? chunks : chunks.subList(0, chunks.size() - 1)).toArray(String[]::new);
    }

    public PromptLibrary getLibrary(Project project) {
        return PromptLibrary.of(project, TagsUtl.resolvePromptLibrary(project, path).orElseThrow());
    }

}
