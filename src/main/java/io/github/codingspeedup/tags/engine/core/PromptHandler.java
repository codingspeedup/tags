package io.github.codingspeedup.tags.engine.core;

import com.intellij.openapi.progress.ProgressIndicator;

import java.util.Optional;

public interface PromptHandler {

    Optional<GenerationResponse> process(ProgressIndicator indicator);

}
