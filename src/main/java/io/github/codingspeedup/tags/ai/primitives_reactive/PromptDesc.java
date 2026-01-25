package io.github.codingspeedup.tags.ai.primitives_reactive;


import dev.langchain4j.model.input.PromptTemplate;

import java.util.List;

public record PromptDesc(PromptTemplate template, List<String> gateway, List<String> plus) {
}
