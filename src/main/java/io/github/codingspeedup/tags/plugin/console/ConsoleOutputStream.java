package io.github.codingspeedup.tags.plugin.console;

import com.intellij.execution.ui.ConsoleViewContentType;
import org.jspecify.annotations.NonNull;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConsoleOutputStream extends OutputStream {
    private final ConsoleServiceBase consoleService;
    private final ConsoleViewContentType contentType;
    private final StringBuilder buffer = new StringBuilder();

    public ConsoleOutputStream(ConsoleServiceBase consoleService, ConsoleViewContentType contentType) {
        this.consoleService = consoleService;
        this.contentType = contentType;
    }

    @Override
    public void write(int b) {
        char c = (char) b;
        if (c == '\n') {
            flushBuffer();
        } else if (b != '\r') {
            buffer.append(c);
        }
    }

    @Override
    public void write(byte @NonNull [] b, int off, int len) {
        var text = new String(b, off, len, StandardCharsets.UTF_8);
        if (text.contains("\n")) {
            var lines = text.split("\n", -1);
            for (int i = 0; i < lines.length - 1; i++) {
                buffer.append(lines[i]);
                flushBuffer();
            }
            buffer.append(lines[lines.length - 1]);
        } else {
            buffer.append(text);
        }
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    private void flushBuffer() {
        if (!buffer.isEmpty()) {
            consoleService.log(buffer.toString(), contentType);
            buffer.setLength(0);
        }
    }

}
