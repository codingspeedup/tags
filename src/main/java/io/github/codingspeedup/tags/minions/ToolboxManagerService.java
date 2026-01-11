package io.github.codingspeedup.tags.minions;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import groovy.lang.GroovyClassLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service(Service.Level.PROJECT)
public final class ToolboxManagerService {

    public static final String GROOVY_EXTENSION = ".groovy";

    private final Project project;
    private GroovyClassLoader toolboxClassLoader;
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ToolboxManagerService(Project project) {
        this.project = project;
        initializeLoader();
    }

    public static ToolboxManagerService getInstance(Project project) {
        return project.getService(ToolboxManagerService.class);
    }

    public static boolean isGroovy(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(GROOVY_EXTENSION);
    }

    private void initializeLoader() {
        this.toolboxClassLoader = new GroovyClassLoader(getClass().getClassLoader());
        var toolsVirtualFile = PluginUtl.resolveToolboxFolder(project).orElseThrow().getParent();
        var toolsFile = VfsUtil.virtualToIoFile(toolsVirtualFile);
        this.toolboxClassLoader.addClasspath(toolsFile.getAbsolutePath());
    }

    public void reloadIfChanged() {
        lock.writeLock().lock();
        try {
            var groovyFiles = new ArrayList<VirtualFile>();
            collectGroovyFiles(PluginUtl.resolveToolboxFolder(project).orElseThrow(), groovyFiles);
            var dirty = groovyFiles.size() != fileTimestamps.size();
            if (!dirty) {
                for (VirtualFile file : groovyFiles) {
                    var lastKnownTs = fileTimestamps.get(file.getPath());
                    if (lastKnownTs == null || file.getTimeStamp() > lastKnownTs) {
                        dirty = true;
                        break;
                    }
                }
            }
            if (dirty) {
                if (toolboxClassLoader != null) toolboxClassLoader.close();
                fileTimestamps.clear();
                initializeLoader();
                for (var file : groovyFiles) {
                    toolboxClassLoader.parseClass(VfsUtil.loadText(file));
                    fileTimestamps.put(file.getPath(), file.getTimeStamp());
                }
            }
        } catch (IOException e) {
            PluginUtl.reportError(project, "Error reloading toolbox", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void collectGroovyFiles(VirtualFile dir, List<VirtualFile> result) {
        VfsUtilCore.visitChildrenRecursively(dir, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && isGroovy(file.getName())) {
                    result.add(file);
                }
                return true;
            }
        });
    }

    public GroovyClassLoader getActiveLoader() {
        lock.readLock().lock();
        try {
            return toolboxClassLoader;
        } finally {
            lock.readLock().unlock();
        }
    }

}