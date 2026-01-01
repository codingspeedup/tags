package io.github.codingspeedup.tags.engine.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptLibraryTest {

    @Test
    void fromPath() throws IOException {
        var pLib = PromptLibrary.of(Path.of("./src/main/resources/tags/prompts/plugin-internal-prompts-library.yaml"));
        assertEquals(3, pLib.getPrompts().size());
    }

}