package io.github.codingspeedup.tags.integration.groovy;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import groovy.lang.GroovyClassLoader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service(Service.Level.PROJECT)
public final class ToolboxManager {

    private final Project project;
    private GroovyClassLoader sharedLoader;
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ToolboxManager(Project project) {
        this.project = project;
        initializeLoader();
    }

    public static ToolboxManager getInstance(Project project) {
        return project.getService(ToolboxManager.class);
    }

    private void initializeLoader() {
        this.sharedLoader = new GroovyClassLoader(getClass().getClassLoader());
        VirtualFile toolsDir = project.getBaseDir().findChild(".llm-tools");
        if (toolsDir != null) {
            this.sharedLoader.addClasspath(toolsDir.getPath());
        }
    }

    public void reloadIfChanged() {
        lock.writeLock().lock();
        try {
            VirtualFile toolsDir = project.getBaseDir().findChild(".llm-tools");
            if (toolsDir == null || !toolsDir.isDirectory()) return;

            VirtualFile[] children = toolsDir.getChildren();
            boolean changed = false;

            for (VirtualFile file : children) {
                if ("groovy".equals(file.getExtension())) {
                    if (file.getTimeStamp() > fileTimestamps.getOrDefault(file.getPath(), 0L)) {
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                if (sharedLoader != null) sharedLoader.close();
                fileTimestamps.clear();
                initializeLoader();

                for (VirtualFile file : children) {
                    if ("groovy".equals(file.getExtension())) {
                        // Load using the content of the VirtualFile
                        sharedLoader.parseClass(VfsUtil.loadText(file), file.getName());
                        fileTimestamps.put(file.getPath(), file.getTimeStamp());
                    }
                }
            }
        } catch (IOException e) {
            // Handle logging
        } finally {
            lock.writeLock().unlock();
        }
    }

    public GroovyClassLoader getActiveLoader() {
        lock.readLock().lock();
        try {
            return sharedLoader;
        } finally {
            lock.readLock().unlock();
        }
    }
}