package io.github.codingspeedup.tags.handlers;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.ai.composition_reactive.GroovyScriptExecutor;
import io.github.codingspeedup.tags.minions.ToolboxManagerService;
import io.github.codingspeedup.tags.ai.deployment_orchestration.ResponseGateway;
import io.github.codingspeedup.tags.plugin.console.ConsoleOutputStream;
import io.github.codingspeedup.tags.plugin.console.GroovyConsoleService;

import java.io.PrintStream;
import java.util.Optional;

public class GroovyScriptHandler implements ActionHandler {

    private final String scriptName;
    private final String scriptContent;

    public GroovyScriptHandler(String scriptName, String scriptContent) {
        this.scriptName = scriptName;
        this.scriptContent = scriptContent;
    }

    @Override
    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) {
        var groovyConsole = GroovyConsoleService.getInstance(project);
        groovyConsole.clearConsole();

        TagsResult tagsResult;
        try (
                var outStream = new PrintStream(new ConsoleOutputStream(groovyConsole, ConsoleViewContentType.NORMAL_OUTPUT));
                var errStream = new PrintStream(new ConsoleOutputStream(groovyConsole, ConsoleViewContentType.ERROR_OUTPUT))
        ) {
            var toolboxManager = ToolboxManagerService.getInstance(project);
            toolboxManager.reloadIfChanged();

            var executor = new GroovyScriptExecutor(outStream, errStream, toolboxManager.getActiveLoader());

            var result = executor.execute(scriptContent, scriptName);
            if (result instanceof Exception e) {
                throw e;
            }

            if (result != null) {
                groovyConsole.info(String.format("Groovy script result:\n%s", result));
            }

            tagsResult = new TagsResult(ResponseGateway.INFO);
            tagsResult.setContent("Groovy script completed successfully.");
        } catch (Exception e) {
            groovyConsole.error("Groovy script error:", e);
            tagsResult = new TagsResult(ResponseGateway.ERROR);
            tagsResult.setContent("Groovy script completed with error:\n" + e.getMessage());
        }
        return Optional.of(tagsResult);
    }

}
