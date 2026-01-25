package io.github.codingspeedup.tags.integration.groovy;

import io.github.codingspeedup.tags.ai.composition_reactive.GroovyScriptExecutor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class GroovyScriptExecutorTest {

    @Test
    void execute() {

        var outContent = new ByteArrayOutputStream();
        var errContent = new ByteArrayOutputStream();

        var executor = new GroovyScriptExecutor(new PrintStream(outContent), new PrintStream(errContent));
        var script = """
                println "Hello to StdOut"
                System.err.println "Hello to StdErr"
            """;

        executor.execute(script);

        var capturedOut = outContent.toString().trim();
        var capturedErr = errContent.toString().trim();

        assertTrue(capturedOut.contains("Hello to StdOut"));
        assertTrue(capturedErr.contains("Hello to StdErr"));

        System.out.println("Test Passed! Captured Out: " + capturedOut);
        System.out.println("Test Passed! Captured Err: " + capturedErr);
    }

}