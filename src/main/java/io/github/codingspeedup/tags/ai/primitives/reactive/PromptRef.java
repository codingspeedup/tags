package io.github.codingspeedup.tags.ai.primitives.reactive;

import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Locale;

import static io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl.PLUGIN_PROMPT_LIBRARY_EXTENSION;

public class PromptRef {

    @Getter
    private final String[] path;

    @Getter
    private final String id;

    public PromptRef(String[] path, String promptId) {
        this.path = new String[path.length];
        this.id = promptId;
        System.arraycopy(path, 0, this.path, 0, path.length);
        var last = this.path[this.path.length - 1];
        if (last.toLowerCase(Locale.ROOT).endsWith(PLUGIN_PROMPT_LIBRARY_EXTENSION)) {
            last = FilenameUtils.getBaseName(last);
        }
        this.path[this.path.length - 1] = last;
    }

    public PromptRef(String ref) {
        ref = StringUtils.trimToEmpty(ref);
        if (ref.startsWith(BufferModel.PROMPT_REF_PREFIX)) {
            ref = ref.substring(1);
        }
        var chunks = Arrays.stream(ref.split("\\."))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotEmpty)
                .toList();
        this.id = ref.endsWith(".") ? StringUtils.EMPTY : chunks.get(chunks.size() - 1);
        this.path = (ref.endsWith(".") ? chunks : chunks.subList(0, chunks.size() - 1)).toArray(String[]::new);
    }


    @Override
    public String toString() {
        return BufferModel.PROMPT_REF_PREFIX + String.join(".", path) + "." + id;
    }

}
