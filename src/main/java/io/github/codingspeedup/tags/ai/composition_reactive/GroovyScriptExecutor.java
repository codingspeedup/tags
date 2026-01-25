package io.github.codingspeedup.tags.ai.composition_reactive;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import lombok.Getter;

import java.io.PrintStream;

public class GroovyScriptExecutor {

    @Getter
    private final PrintStream stdOut;
    @Getter
    private final PrintStream stdErr;
    private final GroovyShell shell;

    public GroovyScriptExecutor(PrintStream stdOut, PrintStream stdErr) {
        this(stdOut, stdErr, null);
    }

    public GroovyScriptExecutor(PrintStream stdOut, PrintStream stdErr, ClassLoader classLoader) {
        this.stdOut = stdOut == null ? System.out : stdOut;
        this.stdErr = stdErr == null ? System.err : stdErr;
        var binding = new Binding();
        binding.setProperty("out", this.stdOut);
        binding.setProperty("err", this.stdErr);
        shell = new GroovyShell(classLoader, binding);
    }

    public void execute(String script) {
        execute(script, "Snippet1");
    }

    public Object execute(String script, String scriptName) {
        var originalOut = System.out;
        var originalErr = System.err;
        try {
            var codeSource = new GroovyCodeSource(script, scriptName, GroovyShell.DEFAULT_CODE_BASE);
            codeSource.setCachable(false);
            System.setErr(stdErr);
            return shell.evaluate(codeSource);
        } catch (Exception ex) {
            return ex;
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }
    }

}
