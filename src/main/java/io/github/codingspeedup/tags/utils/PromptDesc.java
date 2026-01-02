package io.github.codingspeedup.tags.utils;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Locale;

public class PromptDesc {

    public static final String TEMPLATE_PREFIX = "@";

    public static final String VAR_SEPARATOR = "=";
    public static final String VAR_PLACEHOLDER = "∅";

    public static final String SECTION_NAME_START = "<";
    public static final String SECTION_NAME_END = ">";
    public static final String SECTION_CLOSE = "/";

    public static final String SECTION_ROOT_ID = "tags+";

    public static final String SECTION_REF_MARKER = "#";

    private final String[] path;
    @Getter
    private final String id;

    public PromptDesc(String[] path, String promptId) {
        this.path = new String[path.length];
        this.id = promptId;
        System.arraycopy(path, 0, this.path, 0, path.length);
        var last = this.path[this.path.length - 1];
        if (last.toLowerCase(Locale.ROOT).endsWith(".yaml")) {
            last = FilenameUtils.getBaseName(last);
        }
        if (StringUtils.equalsIgnoreCase(TagsUtl.PLUGIN_PROMPT_LIBRARY, last)) {
            last = TagsUtl.PLUGIN_PROMPT_LIBRARY_REF;
        }
        this.path[this.path.length - 1] = last;
    }

    public PromptDesc(String ref) {
        ref = StringUtils.trimToEmpty(ref);
        if (ref.startsWith(TEMPLATE_PREFIX)) {
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

    public String templateRef() {
        return TEMPLATE_PREFIX + String.join(".", path) + "." + id;
    }

}
