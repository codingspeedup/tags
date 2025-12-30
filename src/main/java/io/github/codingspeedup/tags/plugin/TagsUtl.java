package io.github.codingspeedup.tags.plugin;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
                var tagsRootSegment = ".tags";
                var tagsRoot = result[0].findChild(tagsRootSegment);
                if (tagsRoot == null) {
                    tagsRoot = getOrCreateChild(result[0], tagsRootSegment);
                    generateGitignore(project, tagsRoot);
                }
                result[0] = tagsRoot;

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

    private static void generateGitignore(Project project, VirtualFile tagsRoot) throws IOException {
        var gitignoreFile = tagsRoot.createChildData(project, ".gitignore");
        gitignoreFile.setBinaryContent("*\n".getBytes(StandardCharsets.UTF_8));
    }

    public static void sendToClipboard(Project project, GenerationResponse gr) {
        CopyPasteManager.getInstance().setContents(new StringSelection(gr.getGeneratedContent()));
        NotificationGroupManager.getInstance()
                .getNotificationGroup("GenerationGroup")
                .createNotification(
                        "Copied to Clipboard",
                        "Content successfully sent to system clipboard.",
                        NotificationType.INFORMATION
                )
                .notify(project);
    }

    public static void openReadOnlyBuffer(Project project, GenerationResponse gr) {
        var lvf = new LightVirtualFile(
                gr.getBufferName(),
                FileTypeManager.getInstance().getFileTypeByExtension(FilenameUtils.getExtension(gr.getBufferName())),
                gr.getGeneratedContent()
        );
        lvf.setWritable(false);
        var fileEditors = FileEditorManager.getInstance(project).openFile(lvf, true);
        if (fileEditors.length > 0 && fileEditors[0] instanceof TextEditor textEditor) {
            var editor = textEditor.getEditor();
            editor.getCaretModel().moveToOffset(gr.getStartOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    public static void updateEditorDocument(Project project, Editor editor, Document document, GenerationResponse gr) {
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(gr.getGeneratedContent()));

        ApplicationManager.getApplication().invokeLater(() -> {
            editor.getCaretModel().moveToOffset(gr.getStartOffset());
            editor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER);
        });
    }

}
