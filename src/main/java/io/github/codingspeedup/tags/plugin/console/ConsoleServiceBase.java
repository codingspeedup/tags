package io.github.codingspeedup.tags.plugin.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class ConsoleServiceBase {

    protected record DelayedMessage(String message, ConsoleViewContentType type) {
    }

    private volatile ConsoleView consoleView;
    private final List<TagsConsoleService.DelayedMessage> buffer = new ArrayList<>();

    public void setConsoleView(ConsoleView consoleView) {
        synchronized (buffer) {
            this.consoleView = consoleView;
            for (TagsConsoleService.DelayedMessage dm : buffer) {
                printToConsole(this.consoleView, dm.message, dm.type);
            }
            buffer.clear();
        }
    }

    public void log(String message, ConsoleViewContentType type) {
        synchronized (buffer) {
            if (this.consoleView == null) {
                buffer.add(new DelayedMessage(message, type));
            } else {
                printToConsole(this.consoleView, message, type);
            }
        }
    }

    private static void printToConsole(ConsoleView view, String message, ConsoleViewContentType type) {
        if (view == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> view.print(message + "\n", type));
    }

    public void clearConsole() {
        var currentView = this.consoleView;
        if (currentView != null) {
            ApplicationManager.getApplication().invokeLater(currentView::clear);
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

}
