package io.github.codingspeedup.tags.utils;

import codegen.java.TypeGenerator;
import dev.langchain4j.internal.Json;
import org.junit.jupiter.api.Test;

class PromptUtlTest {

    @Test
    void buildToolSpec() {
        var tools = PromptUtl.buildToolSpec(TypeGenerator.class.getName()).orElseThrow();
        var toolsSpec = Json.toJson(tools);
        System.out.printf(toolsSpec);
    }

}