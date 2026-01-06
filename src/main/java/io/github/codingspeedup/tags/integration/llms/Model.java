package io.github.codingspeedup.tags.integration.llms;

import io.github.codingspeedup.tags.plugin.TagsSettings;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public record Model(String name, LLM provider) {

    public Model(String name, LLM provider) {
        this.name = StringUtils.trimToEmpty(name);
        this.provider = provider;
    }

    public boolean isSystemRoleSupported() {
        return !name.toLowerCase().contains("gemma");
    }

    public static Optional<Model> of(String modelHint) {
        var settings = TagsSettings.getInstance();
        if (StringUtils.isBlank(modelHint)) {
            if (settings.isUseAzureOpenAiModel()) {
                return Optional.of(new Model(settings.getAzureOpenAiDeployment(), new AzureOpenAI()));
            }
            if (StringUtils.isNotBlank(settings.getGeminiModel())) {
                return Optional.of(new Model(settings.getGeminiModel(), new GoogleAI()));
            }
            if (StringUtils.isNotBlank(settings.getOllamaModel())) {
                return Optional.of(new Model(settings.getOllamaModel(), new OllamaAI()));
            }
        }
        return Optional.empty();
    }

}
