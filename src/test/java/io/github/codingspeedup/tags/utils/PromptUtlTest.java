package io.github.codingspeedup.tags.utils;

import dev.langchain4j.internal.Json;
import io.github.codingspeedup.tags.prompting.chat.PromptUtl;
import io.github.codingspeedup.tags.prompting.tools.PromptApiSpecBuilder;
import org.junit.jupiter.api.Test;
import tools.codegen.java.TypeAugmenter;
import tools.codegen.java.TypeGenerator;

class PromptUtlTest {

    @Test
    void buildToolSpec() {
        var tools = PromptUtl.buildToolSpec(TypeGenerator.class.getName()).orElseThrow();
        var toolsSpec = Json.toJson(tools);
        System.out.printf(toolsSpec);
    }

    @Test
    void buildToolMarkdown() {
        var md = PromptApiSpecBuilder.of(null, TypeAugmenter.class.getName(), TypeGenerator.class.getName()).orElseThrow();
        System.out.printf(md);
    }

}