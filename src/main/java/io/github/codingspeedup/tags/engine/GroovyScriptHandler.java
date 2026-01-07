package io.github.codingspeedup.tags.engine;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.integration.groovy.ScriptExecutor;
import io.github.codingspeedup.tags.plugin.console.ConsoleOutputStream;
import io.github.codingspeedup.tags.plugin.console.TagsConsoleService;
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
    public Optional<TagsResult> process(Project project, ProgressIndicator indicator) throws Exception {
        var pluginConsole = TagsConsoleService.getInstance(project);
        try (
                var outStream = new PrintStream(new ConsoleOutputStream(pluginConsole, ConsoleViewContentType.NORMAL_OUTPUT));
                var errStream = new PrintStream(new ConsoleOutputStream(pluginConsole, ConsoleViewContentType.ERROR_OUTPUT))
        ) {
            var executor = new ScriptExecutor(outStream, errStream);
            var result = executor.execute(script);
            if (result instanceof Exception e) {
                throw e;
            }
            var tagsResult = new TagsResult(ActionResultGateway.INFO);
            if (result != null) {
                tagsResult.setContent(String.valueOf(result));
            } else {
                tagsResult.setContent("Groovy script execution succeeded");
            }
            return Optional.of(tagsResult);
        }
    }

}
