package io.github.codingspeedup.tags.ai.primitives_models;

import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
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

    public static Optional<Model> of(EnvironmentSettingsProvider settings, String modelHint) {
        if (StringUtils.isBlank(modelHint)) {
            if (settings.isUseAzureOpenAiModel()) {
                return Optional.of(new Model(settings.getAzureOpenAiDeployment(), new AzureOpenAI(settings)));
            }
            if (StringUtils.isNotBlank(settings.getGeminiModel())) {
                return Optional.of(new Model(settings.getGeminiModel(), new GoogleAI(settings)));
            }
            if (StringUtils.isNotBlank(settings.getOllamaModel())) {
                return Optional.of(new Model(settings.getOllamaModel(), new OllamaAI(settings)));
            }
        }
        return Optional.empty();
    }

}
