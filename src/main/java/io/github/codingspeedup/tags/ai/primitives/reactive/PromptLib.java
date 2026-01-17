package io.github.codingspeedup.tags.ai.primitives.reactive;

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

import static io.github.codingspeedup.tags.ai.primitives.reactive.PromptLibUtl.mergeToProperties;

@Getter
public class PromptLib {

    private final String name;
    private final Properties parameters = new Properties();
    private final Properties defaults = new Properties();
    private final PromptTemplate systemTemplate;
    private final Map<String, PromptTemplate> prompts = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public PromptLib(String name, Map<String, Object> data) {
        this.name = name;

        mergeToProperties(this.parameters, (Map<String, Object>) data.get("parameters"));

        mergeToProperties(this.defaults, (Map<String, Object>) data.get("defaults"));

        systemTemplate = PromptTemplate.from(StringUtils.trimToEmpty((String) data.get("system")), this.name);

        var prompts = (List<Map<String, Object>>) data.get("prompts");
        prompts.forEach(prompt -> {
            var id = StringUtils.trimToEmpty((String) prompt.get("id"));
            var template = (String) prompt.get("template");
            this.prompts.put(id, PromptTemplate.from(template, this.name + "." + id));
        });
    }

    public Optional<PromptTemplate> getPromptTemplate(String promptId) {
        return Optional.ofNullable(prompts.get(promptId));
    }

    public Set<String> getVariables() {
        return PromptUtl.findVariables(systemTemplate);
    }

    public Set<String> getVariables(String promptId) {
        var vars = getVariables();
        vars.addAll(PromptUtl.findVariables(getPromptTemplate(promptId).orElseThrow()));
        return vars;
    }

    public static PromptLib of(Path libraryPath) throws IOException {
        var name = StringUtils.trimToEmpty(FilenameUtils.getBaseName(libraryPath.getFileName().toString()));
        try (var inputStream = Files.newInputStream(libraryPath)) {
            var loaderOptions = new LoaderOptions();
            var yaml = new Yaml(new SafeConstructor(loaderOptions));
            @SuppressWarnings("unchecked")
            var data = (Map<String, Object>) yaml.load(inputStream);
            return new PromptLib(name, data);
        }
    }

}
