package io.github.codingspeedup.tags.utils;

import dev.langchain4j.internal.Json;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptUtl;
import io.github.codingspeedup.tags.ai.composition.reactive.ToolboxApiSpecBuilder;
import org.junit.jupiter.api.Test;
import tools.builtin.codegen.java.TypeAugmenter;
import tools.builtin.codegen.java.TypeGenerator;

class PromptUtlTest {

    @Test
    void buildToolSpec() {
        var tools = PromptUtl.buildToolSpec(TypeGenerator.class.getName()).orElseThrow();
        var toolsSpec = Json.toJson(tools);
        System.out.printf(toolsSpec);
    }

    @Test
    void buildToolMarkdown() {
        var md = ToolboxApiSpecBuilder.of(null, TypeAugmenter.class.getName(), TypeGenerator.class.getName()).orElseThrow();
        System.out.printf(md);
    }

}