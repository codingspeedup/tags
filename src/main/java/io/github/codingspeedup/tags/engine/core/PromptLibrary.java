package io.github.codingspeedup.tags.engine.core;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class PromptLibrary {

    private final String name;
    private Properties parametes = new Properties();
    private SystemMessage systemMessage;
    private Map<String, UserMessage> prompts;

    public PromptLibrary(String name) {
        this.name = name;
    }

    public PromptLibrary(Path yamlPath) throws IOException {
        this.name = FilenameUtils.getBaseName(yamlPath.getFileName().toString());
        try (var inputStream = Files.newInputStream(yamlPath)) {
            var data = new Yaml().load(inputStream);
            System.out.println(data);
        }
    }

}
