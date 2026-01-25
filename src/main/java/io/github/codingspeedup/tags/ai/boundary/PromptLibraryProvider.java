package io.github.codingspeedup.tags.ai.boundary;

import io.github.codingspeedup.tags.ai.primitives_reactive.PromptLib;
import io.github.codingspeedup.tags.ai.primitives_reactive.PromptRef;

import java.util.Optional;

public interface PromptLibraryProvider {

    Optional<PromptLib> load(PromptRef promptRef);

}
