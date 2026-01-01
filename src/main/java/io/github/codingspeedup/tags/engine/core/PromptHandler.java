package io.github.codingspeedup.tags.engine.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public interface PromptHandler {

    Optional<TagsResult> process(Project project, ProgressIndicator indicator);

}
