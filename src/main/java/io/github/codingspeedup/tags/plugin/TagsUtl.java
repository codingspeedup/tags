package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagsUtl {

    public static Optional<VirtualFile> getPluginFolder(@NotNull Project project, String... path) {
        var logger = project.getService(TagsConsoleService.class);

        var projectRoot = ProjectUtil.guessProjectDir(project);
        if (projectRoot == null) {
            logger.error("Project root not found");
            return Optional.empty();
        }

        final var pluginFolder = new VirtualFile[]{projectRoot};
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                var tagsRoot = ".tags";
                var child = pluginFolder[0].findChild(tagsRoot);
                if (child == null) {
                    child = pluginFolder[0].createChildDirectory(TagsUtl.class, tagsRoot);
                }
                pluginFolder[0] = child;

                for (var segment : path) {
                    if (StringUtils.isBlank(segment)) {
                        continue;
                    }
                    child = pluginFolder[0].findChild(segment);
                    if (child == null) {
                        child = pluginFolder[0].createChildDirectory(TagsUtl.class, segment);
                    }
                    pluginFolder[0] = child;
                }
            } catch (IOException e) {
                logger.error("Failed to create folder", e);
                pluginFolder[0] = null;
            }
        });

        return pluginFolder[0] == null ? Optional.empty() : Optional.of(pluginFolder[0]);
    }

    public static Optional<VirtualFile> getChatFolder(@NotNull Project project) {
        return getPluginFolder(project, "chat");
    }

}
