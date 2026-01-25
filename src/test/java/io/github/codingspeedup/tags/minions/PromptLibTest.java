package io.github.codingspeedup.tags.minions;

import io.github.codingspeedup.tags.ai.primitives_reactive.PromptLib;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.codingspeedup.tags.ai.primitives_reactive.PromptLibUtl.PLUGIN_PROMPT_LIBRARY_EXTENSION;
import static io.github.codingspeedup.tags.ai.primitives_reactive.PromptLibUtl.PLUGIN_PROMPT_LIBRARY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptLibTest {

    @Test
    void fromPath() throws IOException {
        var pLib = PromptLib.of(Path.of("./src/main/resources/tags/prompts/",
                String.format("%s%s", PLUGIN_PROMPT_LIBRARY_NAME, PLUGIN_PROMPT_LIBRARY_EXTENSION)));
        assertEquals(3, pLib.getPrompts().size());
    }

}