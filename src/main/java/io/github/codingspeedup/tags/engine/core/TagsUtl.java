package io.github.codingspeedup.tags.engine.core;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import io.github.codingspeedup.tags.plugin.TagsConsoleService;
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
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagsUtl {

    public static final String PLUGIN_PROMPT_LIBRARY_REF = "~";

    private static final String PLUGIN_PROMPT_LIBRARY = "plugin-internal-prompts-library";

    public static TagsConsoleService getLogger(Project project) {
        return project.getService(TagsConsoleService.class);
    }

    public static Optional<VirtualFile> resolveChatFolder(@NotNull Project project) {
        return resolvePluginFolder(project, "chat");
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

        var fileName = PLUGIN_PROMPT_LIBRARY;
        if (folders.size() > 1) {
            fileName = folders.get(folders.size() - 1);
            folders.remove(folders.size() - 1);
            if (PLUGIN_PROMPT_LIBRARY_REF.equals(fileName)) {
                fileName = PLUGIN_PROMPT_LIBRARY;
            }
        }

        var folder = resolvePluginFolder(project, folders.toArray(new String[]{}));
        if (folder.isEmpty()) {
            return Optional.empty();
        }

        if (!fileName.toLowerCase().endsWith(".yaml")) {
            fileName = fileName + ".yaml";
        }

        var libraryFile = folder.get().findChild(fileName);
        if (libraryFile == null) {
            final var finalFileName = fileName;
            final var finalResult = new VirtualFile[]{null};
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    var yamlFile = folder.get().createChildData(project, finalFileName);

                    var yamlContent = StringUtils.EMPTY;
                    if (finalFileName.equals(PLUGIN_PROMPT_LIBRARY + ".yaml")) {
                        var resourcePath = "tags/prompts/" + finalFileName;
                        try (var inputStream = TagsUtl.class.getClassLoader().getResourceAsStream(resourcePath)) {
                            if (inputStream == null) {
                                throw new RuntimeException("Resource not found: " + resourcePath);
                            }
                            yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } else {
                        yamlContent = """
                                parameters: {}
                                
                                system: |
                                  You are a helpful and precise AI assistant.
                                  Provide clear, accurate, and direct responses to the user's instructions.
                                
                                prompts:
                                  - id: "loremIpsum"
                                    template: |
                                      Lorem ipsum...
                                
                                """;
                    }
                    yamlFile.setBinaryContent(yamlContent.getBytes(StandardCharsets.UTF_8));

                    FileDocumentManager.getInstance().saveAllDocuments();
                    finalResult[0] = yamlFile;
                } catch (IOException e) {
                    getLogger(project).error("Failed to create prompt template library " + finalFileName, e);
                    finalResult[0] = null;
                }
            });
            libraryFile = finalResult[0];
        }

        return Optional.ofNullable(libraryFile);
    }

    public static Optional<VirtualFile> resolvePluginFolder(@NotNull Project project, String... path) {
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
                getLogger(project).error("Failed to create folder", e);
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
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    public static void sendToClipboard(Project project, TagsResult tagsResult) {
        CopyPasteManager.getInstance().setContents(new StringSelection(tagsResult.getContent()));
        var notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("GenerationGroup")
                .createNotification(
                        "Copied to Clipboard",
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

    public static void openReadOnlyBuffer(Project project, TagsResult tagsResult) {
        var lvf = new LightVirtualFile(
                tagsResult.getBufferName(),
                FileTypeManager.getInstance().getFileTypeByExtension(FilenameUtils.getExtension(tagsResult.getBufferName())),
                tagsResult.getContent()
        );
        lvf.setWritable(false);
        var fileEditors = FileEditorManager.getInstance(project).openFile(lvf, true);
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

}
