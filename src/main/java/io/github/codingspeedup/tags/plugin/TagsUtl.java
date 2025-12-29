package io.github.codingspeedup.tags.plugin;

import com.intellij.openapi.command.WriteCommandAction;
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

    public static TagsConsoleService getLogger(Project project) {
        return project.getService(TagsConsoleService.class);
    }

    public static Optional<VirtualFile> getChatFolder(@NotNull Project project) {
        return getPluginFolder(project, "chat");
    }

    public static Optional<VirtualFile> getPluginFolder(@NotNull Project project, String... path) {
        var logger = getLogger(project);
        var projectRoot = ProjectUtil.guessProjectDir(project);

        if (projectRoot == null) return Optional.empty();

        final var result = new VirtualFile[]{projectRoot};

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                var tagsRoot = ".tags";
                result[0] = getOrCreateChild(result[0], tagsRoot);

                for (var segment : path) {
                    if (StringUtils.isNotBlank(segment)) {
                        result[0] = getOrCreateChild(result[0], segment);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to create folder", e);
                result[0] = null;
            }
        });

        return Optional.ofNullable(result[0]);
    }

    private static VirtualFile getOrCreateChild(VirtualFile parent, String name) throws IOException {
        var child = parent.findChild(name);
        return (child != null) ? child : parent.createChildDirectory(TagsUtl.class, name);
    }


}
