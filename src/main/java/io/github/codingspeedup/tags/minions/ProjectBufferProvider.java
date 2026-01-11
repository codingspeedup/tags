package io.github.codingspeedup.tags.minions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.codingspeedup.tags.ai.boundary.BufferProvider;

import java.nio.file.Path;
import java.util.Optional;

public class ProjectBufferProvider implements BufferProvider {

    private final Project project;
    private final VirtualFile virtualFile;
    private String content;

    public ProjectBufferProvider(Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
    }

    public ProjectBufferProvider(Project project, VirtualFile virtualFile, String content) {
        this(project, virtualFile);
        this.content = content;
    }

    @Override
    public Path getPath() {
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.getFileSystem().getNioPath(virtualFile);
    }

    @Override
    public synchronized String getContent() {
        if (content == null) {
            content = PluginUtl.readText(project, virtualFile).orElseThrow();
        }
        return content;
    }

    @Override
    public Optional<BufferProvider> resolve(String bufferRef) {
        VirtualFile refVirtualFile;
        if (bufferRef.startsWith("/")) {
            refVirtualFile = LocalFileSystem.getInstance().findFileByPath(bufferRef);
        } else if (bufferRef.startsWith("./") || bufferRef.startsWith("../")) {
            refVirtualFile = virtualFile.getParent().findFileByRelativePath(bufferRef);
        } else {
            var projectRoot = PluginUtl.resolveProjectRoot(project);
            refVirtualFile = (projectRoot == null) ? null : projectRoot.findFileByRelativePath(bufferRef);
        }
        if (refVirtualFile != null) {
            return Optional.of(new ProjectBufferProvider(project, refVirtualFile));
        }
        return Optional.empty();
    }

}
