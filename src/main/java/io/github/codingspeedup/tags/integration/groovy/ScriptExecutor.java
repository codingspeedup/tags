package io.github.codingspeedup.tags.integration.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.Getter;

import java.io.PrintStream;

public class ScriptExecutor {

    @Getter
    private final PrintStream stdOut;
    @Getter
    private final PrintStream stdErr;
    private final GroovyShell shell;

    public ScriptExecutor(PrintStream stdOut, PrintStream stdErr) {
        this(stdOut, stdErr, null);
    }

    public ScriptExecutor(PrintStream stdOut, PrintStream stdErr, ClassLoader classLoader) {
        this.stdOut = stdOut == null ? System.out : stdOut;
        this.stdErr = stdErr == null ? System.err : stdErr;
        var binding = new Binding();
        binding.setProperty("out", this.stdOut);
        binding.setProperty("err", this.stdErr);
        shell = new GroovyShell(classLoader, binding);
    }

    public Object execute(String script) {
        var originalOut = System.out;
        var originalErr = System.err;
        try {
            System.setErr(stdErr);
            return shell.evaluate(script);
        } catch (Exception ex) {
            return ex;
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }
    }

}
