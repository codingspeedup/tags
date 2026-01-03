package io.github.codingspeedup.tags.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class GroovyUtlTest {

    @Test
    void testScriptOutput() {
        // 1. Setup buffers for capturing
        var outContent = new ByteArrayOutputStream();
        var errContent = new ByteArrayOutputStream();

        // Save original stderr to restore later
        var originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try {
            // 2. Setup Binding for stdout
            var binding = new Binding();
            binding.setProperty("out", new PrintStream(outContent));

            var shell = new GroovyShell(binding);
            var script = """
                println "Hello to StdOut"
                System.err.println "Hello to StdErr"
            """;

            // 3. Execute
            shell.evaluate(script);

            // 4. Assertions
            var capturedOut = outContent.toString().trim();
            var capturedErr = errContent.toString().trim();

            assertTrue(capturedOut.contains("Hello to StdOut"));
            assertTrue(capturedErr.contains("Hello to StdErr"));

            System.out.println("Test Passed! Captured Out: " + capturedOut);
        } finally {
            // Always restore System.err
            System.setErr(originalErr);
        }
    }

}