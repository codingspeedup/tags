package io.github.codingspeedup.tags.engine;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.integration.groovy.ScriptExecutor;
import io.github.codingspeedup.tags.integration.groovy.ToolboxManagerService;
import io.github.codingspeedup.tags.plugin.console.ConsoleOutputStream;
import io.github.codingspeedup.tags.plugin.console.GroovyConsoleService;
import io.github.codingspeedup.tags.utils.ActionResultGateway;
import io.github.codingspeedup.tags.utils.PromptHandler;
import io.github.codingspeedup.tags.utils.TagsResult;

import java.io.PrintStream;
import java.util.Optional;

public class GroovyScriptHandler implements PromptHandler {

    private final String script;

    public GroovyScriptHandler(String script) {
        this.script = script;
    }

    @Override
    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var groovyConsole = GroovyConsoleService.getInstance(project);
        groovyConsole.clearConsole();

        try (
                var outStream = new PrintStream(new ConsoleOutputStream(groovyConsole, ConsoleViewContentType.NORMAL_OUTPUT));
                var errStream = new PrintStream(new ConsoleOutputStream(groovyConsole, ConsoleViewContentType.ERROR_OUTPUT))
        ) {
            var toolboxManager = ToolboxManagerService.getInstance(project);
            toolboxManager.reloadIfChanged();

            var executor = new ScriptExecutor(outStream, errStream, toolboxManager.getActiveLoader());

            var result = executor.execute(script);
            if (result instanceof Exception e) {
                throw e;
            }

            if (result != null) {
                groovyConsole.info(String.format("Groovy script result:\n%s", result));
            }
        } catch (Exception e) {
            groovyConsole.error("Groovy script error:", e);
        }

        groovyConsole.info("Done!");
        return Optional.of(new TagsResult(ActionResultGateway.IGNORE));
    }

}
