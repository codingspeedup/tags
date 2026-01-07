package io.github.codingspeedup.tags.plugin.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class TagsConsoleService {

    public static TagsConsoleService getInstance(Project project) {
        return project.getService(TagsConsoleService.class);
    }

    private record DelayedMessage(String message, ConsoleViewContentType type) {
    }

    private ConsoleView consoleView;
    private final List<DelayedMessage> buffer = new ArrayList<>();

    @SuppressWarnings("unused")
    public void setConsoleView(ConsoleView consoleView) {
        this.consoleView = consoleView;
        synchronized (buffer) {
            for (DelayedMessage dm : buffer) {
                printToConsole(this.consoleView, dm.message, dm.type);
            }
            buffer.clear();
        }
    }

    public void info(String message) {
        log(message, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public void warn(String message) {
        log(message, ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }

    public void error(String message) {
        log(message, ConsoleViewContentType.ERROR_OUTPUT);
    }

    public void error(String message, Throwable throwable) {
        error(message);
        error(ExceptionUtil.getThrowableText(throwable));
    }

    void log(String message, ConsoleViewContentType type) {
        var currentView = this.consoleView;
        if (currentView == null) {
            synchronized (buffer) {
                buffer.add(new DelayedMessage(message, type));
            }
        } else {
            printToConsole(currentView, message, type);
        }
    }

    private static void printToConsole(ConsoleView view, String message, ConsoleViewContentType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (view != null && view.getComponent().isDisplayable()) {
                view.print(message + "\n", type);
            }
        });
    }

}
