package io.github.codingspeedup.tags.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.codingspeedup.tags.plugin.core.TagsUtl;
import io.github.codingspeedup.tags.prompting.plib.PromptLib;
import io.github.codingspeedup.tags.prompting.plib.PromptRef;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.StreamSupport;

public class TagsEditInsertTemplateFromLibraryGroup extends DefaultActionGroup {

    private final VirtualFile libFile;

    @SuppressWarnings("unused")
    public TagsEditInsertTemplateFromLibraryGroup() {
        this.libFile = null;
    }

    public TagsEditInsertTemplateFromLibraryGroup(String name, VirtualFile libFile) {
        super(name, true);
        this.libFile = libFile;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(TagsEditActionBase.isAvailable(e));
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        var project = e.getProject();
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        var libRoot = TagsUtl.resolvePromptLibrary(project).orElse(null);
        if (libRoot == null) {
            return AnAction.EMPTY_ARRAY;
        }

        libRoot = libRoot.getParent();

        if (libFile == null) {
            return mapFolderToActions(libRoot);
        } else if (libFile.isDirectory()) {
            return mapFolderToActions(libFile);
        } else {
            var relativePath = libRoot.toNioPath().relativize(libFile.toNioPath());
            var path = StreamSupport.stream(relativePath.spliterator(), false)
                    .map(Path::toString)
                    .toArray(String[]::new);

            var pLib = PromptLib.of(project, libFile);
            return pLib.getPrompts().keySet().stream()
                    .sorted()
                    .map(promptId -> new TagsEditInsertTemplateAction(promptId, new PromptRef(path, promptId)))
                    .toArray(AnAction[]::new);
        }
    }

    private AnAction[] mapFolderToActions(VirtualFile promptLibFolder) {
        var actions = new ArrayList<AnAction>();

        var libraries = new ArrayList<VirtualFile>();
        var subFolders = new ArrayList<VirtualFile>();
        Arrays.stream(promptLibFolder.getChildren())
                .filter(VirtualFile::isValid)
                .forEach(vf -> {
                    if (vf.isDirectory()) {
                        subFolders.add(vf);
                    } else if (vf.isValid()) {
                        libraries.add(vf);
                    }
                });

        libraries.forEach(libFile -> {
            var libName = FilenameUtils.getBaseName(libFile.getName());
            actions.add(new TagsEditInsertTemplateFromLibraryGroup(libName, libFile));
        });

        subFolders.forEach(libFile -> actions.add(new TagsEditInsertTemplateFromLibraryGroup("* " + libFile.getName(), libFile)));

        return actions.toArray(new AnAction[]{});
    }

}
