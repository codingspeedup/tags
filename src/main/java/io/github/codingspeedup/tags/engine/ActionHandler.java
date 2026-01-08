package io.github.codingspeedup.tags.engine;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public interface ActionHandler {

    Optional<TagsResult> process(Project project, ProgressIndicator indicator);

}
