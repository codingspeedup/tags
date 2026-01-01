package io.github.codingspeedup.tags.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
public class PromptLibrary {

    private final String name;
    private final Properties parameters = new Properties();
    private final PromptTemplate system;
    private final Map<String, PromptTemplate> prompts = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    private PromptLibrary(String name, Map<String, Object> data) {
        this.name = name;

        var parameters = (Map<String, Object>) data.get("parameters");
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }

        system = PromptTemplate.from(StringUtils.trimToEmpty((String) data.get("system")), this.name);

        var prompts = (List<Map<String, Object>>) data.get("prompts");
        prompts.forEach(prompt -> {
            var id = StringUtils.trimToEmpty((String) prompt.get("id"));
            var template = (String) prompt.get("template");
            this.prompts.put(id, PromptTemplate.from(template, this.name + "." + id));
        });
    }

    public Set<String> getVariables() {
        return PromptUtl.findVariables(system);
    }

    public Set<String> getVariables(String promptId) {
        var vars = getVariables();
        var prompt = prompts.get(promptId);
        if (prompt != null) {
            vars.addAll(PromptUtl.findVariables(prompt));
        }
        return vars;
    }

    public static PromptLibrary of(Path libraryPath) throws IOException {
        var name = StringUtils.trimToEmpty(FilenameUtils.getBaseName(libraryPath.getFileName().toString()));
        try (var inputStream = Files.newInputStream(libraryPath)) {
            var loaderOptions = new LoaderOptions();
            var yaml = new Yaml(new SafeConstructor(loaderOptions));
            @SuppressWarnings("unchecked")
            var data = (Map<String, Object>) yaml.load(inputStream);
            return new PromptLibrary(name, data);
        }
    }

    public static PromptLibrary of(Project project, VirtualFile libraryFile) {
        var name = StringUtils.trimToEmpty(FilenameUtils.getBaseName(libraryFile.getName()));
        var loaderOptions = new LoaderOptions();
        var yaml = new Yaml(new SafeConstructor(loaderOptions));
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) yaml.load(TagsUtl.readText(project, libraryFile).orElseThrow());
        return new PromptLibrary(name, data);
    }

    public static PromptLibrary of(Project project) {
        return PromptLibrary.of(project, TagsUtl.resolvePromptLibrary(project).orElseThrow());
    }

}
