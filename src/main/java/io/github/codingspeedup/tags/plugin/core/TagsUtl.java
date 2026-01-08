package io.github.codingspeedup.tags.plugin.core;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import io.github.codingspeedup.tags.engine.TagsResult;
import io.github.codingspeedup.tags.plugin.console.TagsConsoleService;
import io.github.codingspeedup.tags.prompting.plib.PromptLibUtl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.prompting.plib.PromptLibUtl.PLUGIN_PROMPT_LIBRARY_EXTENSION;
import static io.github.codingspeedup.tags.prompting.plib.PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagsUtl {

    public static void saveAllDocuments() {
        var application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            FileDocumentManager.getInstance().saveAllDocuments();
        } else {
            application.invokeLater(() -> FileDocumentManager.getInstance().saveAllDocuments());
        }
    }

    public static Set<String> getOpenTabNames(Project project) {
        return ReadAction.compute(() -> {
            var manager = FileEditorManager.getInstance(project);
            return Arrays.stream(manager.getOpenFiles())
                    .map(VirtualFile::getName)
                    .collect(Collectors.toSet());
        });
    }

    public static Optional<String> readText(Project project, VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.isValid()) {
            return Optional.empty();
        }

        var app = ApplicationManager.getApplication();

        if (!app.isReadAccessAllowed()) {
            // Refresh outside ReadAction to avoid deadlocks
            virtualFile.refresh(false, false);
        }

        var content = app.runReadAction((Computable<String>) () -> {
            var docManager = FileDocumentManager.getInstance();

            // Use cached document first to avoid unnecessary loading if possible
            var document = docManager.getCachedDocument(virtualFile);
            if (document == null) {
                // This will load the document into memory if not already there
                document = docManager.getDocument(virtualFile);
            }

            if (document != null) {
                return document.getText();
            }

            try {
                // VfsUtil.loadText handles charset and stream closing automatically
                return VfsUtil.loadText(virtualFile);
            } catch (IOException e) {
                reportError(project, String.format("Error reading content from file `%s'", virtualFile.getPath()), e);
                return null;
            }
        });

        return Optional.ofNullable(content);
    }

    public static void writeText(@NotNull Project project, @NotNull VirtualFile file, @NotNull String content) {
        Runnable runnable = () -> {
            try {
                VfsUtil.saveText(file, content);
            } catch (IOException e) {
                reportError(project, String.format("Failed to write `%s'", file.getPath()), e);
            }
        };
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            runnable.run();
        } else {
            WriteCommandAction.runWriteCommandAction(project, runnable);
        }
    }

    public static Optional<VirtualFile> resolvePromptLibrary(@NotNull Project project, String... path) {
        var folders = new ArrayList<String>();
        folders.add("prompts");
        if (!ArrayUtils.isEmpty(path)) {
            Arrays.stream(path)
                    .map(StringUtils::trimToEmpty)
                    .filter(StringUtils::isNotBlank)
                    .forEach(folders::add);
        }

        var fileName = PLUGIN_PROMPT_LIBRARY_NAME;
        if (folders.size() > 1) {
            fileName = folders.get(folders.size() - 1);
            folders.remove(folders.size() - 1);
        }

        var folder = resolvePluginFolder(project, folders.toArray(new String[]{}));
        if (folder.isEmpty()) {
            return Optional.empty();
        }

        if (!fileName.toLowerCase().endsWith(PLUGIN_PROMPT_LIBRARY_EXTENSION)) {
            fileName = fileName + PLUGIN_PROMPT_LIBRARY_EXTENSION;
        }

        var libraryFile = folder.get().findChild(fileName);
        if (libraryFile == null) {
            final var finalFileName = fileName;
            final var finalResult = new VirtualFile[]{null};
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    var yamlFile = folder.get().createChildData(project, finalFileName);

                    var yamlContent = StringUtils.EMPTY;
                    if (finalFileName.equals(PLUGIN_PROMPT_LIBRARY_NAME + PLUGIN_PROMPT_LIBRARY_EXTENSION)) {
                        var resourcePath = "tags/prompts/" + finalFileName;
                        try (var inputStream = TagsUtl.class.getClassLoader().getResourceAsStream(resourcePath)) {
                            if (inputStream == null) {
                                throw new RuntimeException("Resource not found: " + resourcePath);
                            }
                            yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } else {
                        yamlContent = PromptLibUtl.SAMPLE_LIBRARY_CONTENT;
                    }
                    writeText(project, yamlFile, yamlContent);

                    finalResult[0] = yamlFile;
                } catch (IOException e) {
                    reportError(project, String.format("Failed to create prompt template library `%s'", finalFileName), e);
                    finalResult[0] = null;
                }
            });
            libraryFile = finalResult[0];
        }

        return Optional.ofNullable(libraryFile);
    }

    public static Optional<VirtualFile> resolveToolsFolder(@NotNull Project project) {
        return resolvePluginFolder(project, "tools");
    }

    public static Optional<VirtualFile> resolvePluginFolder(@NotNull Project project, String... path) {
        var projectRoot = ProjectUtil.guessProjectDir(project);
        if (projectRoot == null) {
            return Optional.empty();
        }

        final var result = new VirtualFile[]{projectRoot};
        var tagsRootSegment = ".tags";

        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            var current = projectRoot.findChild(tagsRootSegment);
            if (current != null) {
                for (var segment : path) {
                    if (StringUtils.isNotBlank(segment)) {
                        current = current.findChild(segment);
                        if (current == null) {
                            break;
                        }
                    }
                }
                result[0] = current;
            } else {
                result[0] = null;
            }
        } else {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
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
                    reportError(project, "Failed to create folder", e);
                    result[0] = null;
                }
            });
        }

        return Optional.ofNullable(result[0]);
    }

    private static VirtualFile getOrCreateChild(VirtualFile parent, String name) throws IOException {
        var child = parent.findChild(name);
        return (child != null) ? child : parent.createChildDirectory(TagsUtl.class, name);
    }

    private static void generateGitignore(Project project, VirtualFile tagsRoot) throws IOException {
        var gitignoreFile = tagsRoot.createChildData(project, ".gitignore");
        writeText(project, gitignoreFile, "*\n");
    }

    public static void sendToClipboard(Project project, TagsResult tagsResult) {
        CopyPasteManager.getInstance().setContents(new StringSelection(tagsResult.getContent()));
        var notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("TAGS+Group")
                .createNotification(
                        TagsMessageBundle.message("plugin.label"),
                        "Content successfully sent to system clipboard.",
                        NotificationType.INFORMATION
                );
        notification.notify(project);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                TimeUnit.SECONDS.sleep(7);
                ApplicationManager.getApplication().invokeLater(notification::expire);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void openChatBuffer(Project project, TagsResult tagsResult) {
        var chatBuffer = new LightVirtualFile(
                tagsResult.getBufferName(),
                FileTypeManager.getInstance().getFileTypeByExtension(FilenameUtils.getExtension(tagsResult.getBufferName())),
                tagsResult.getContent()
        );
        chatBuffer.setWritable(true);
        var fileEditors = FileEditorManager.getInstance(project).openFile(chatBuffer, true);
        if (fileEditors.length > 0 && fileEditors[0] instanceof TextEditor textEditor) {
            var editor = textEditor.getEditor();
            editor.getCaretModel().moveToOffset(tagsResult.getStartOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    public static void updateEditorDocument(Project project, Editor editor, Document document, TagsResult tagsResult) {
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(tagsResult.getContent()));

        ApplicationManager.getApplication().invokeLater(() -> {
            editor.getCaretModel().moveToOffset(tagsResult.getStartOffset());
            editor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER);
        });
    }

    public static void reportInfo(Project project, String message) {
        reportIssue(project, NotificationType.INFORMATION, message, null);
    }

    public static void reportWarning(Project project, String message) {
        reportIssue(project, NotificationType.WARNING, message, null);
    }

    public static void reportError(Project project, String message) {
        reportError(project, message, null);
    }

    public static void reportError(Project project, String message, Exception ex) {
        reportIssue(project, NotificationType.ERROR, message, ex);
    }

    private static void reportIssue(Project project, NotificationType level, String message, Exception ex) {
        message = StringUtils.trimToEmpty(message);
        if (StringUtils.isEmpty(message) && ex != null) {
            message = ex.getClass().getName();
        }

        switch (level) {
            case ERROR -> {
                if (ex == null) {
                    TagsConsoleService.getInstance(project).error(message);
                } else {
                    TagsConsoleService.getInstance(project).error(message, ex);
                }
            }
            case WARNING -> TagsConsoleService.getInstance(project).warn(message);
            default -> TagsConsoleService.getInstance(project).info(message);
        }

        var notificationMessage = message;
        if (ex != null) {
            message = ex.getMessage();
            if (StringUtils.isBlank(message)) {
                notificationMessage += ": " + ex.getClass().getSimpleName();
            } else {
                notificationMessage += ":\n" + message;
            }
        }

        NotificationGroupManager.getInstance()
                .getNotificationGroup("TAGS+Group")
                .createNotification(
                        TagsMessageBundle.message("plugin.label"),
                        notificationMessage,
                        level
                ).notify(project);
    }


}