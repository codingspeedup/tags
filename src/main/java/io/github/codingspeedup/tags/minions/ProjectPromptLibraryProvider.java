package io.github.codingspeedup.tags.minions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.codingspeedup.tags.ai.boundary.PromptLibraryProvider;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLib;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class ProjectPromptLibraryProvider implements PromptLibraryProvider {

    private final Project project;

    @Override
    public Optional<PromptLib> load(PromptRef promptRef) {
        return load(promptRef.getPath());
    }

    public Optional<PromptLib> load(String... path) {
        return PluginUtl.resolvePromptLibrary(project, path)
                .flatMap(this::load);
    }

    private Optional<PromptLib> load(VirtualFile libraryFile) {
        var name = StringUtils.trimToEmpty(FilenameUtils.getBaseName(libraryFile.getName()));
        var loaderOptions = new LoaderOptions();
        var yaml = new Yaml(new SafeConstructor(loaderOptions));
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) yaml.load(PluginUtl.readText(project, libraryFile).orElseThrow());
        return Optional.of(new PromptLib(name, data));
    }

}
