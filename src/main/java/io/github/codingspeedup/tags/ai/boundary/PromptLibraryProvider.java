package io.github.codingspeedup.tags.ai.boundary;

import io.github.codingspeedup.tags.ai.primitives.reactive.PromptLib;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;

import java.util.Optional;

public interface PromptLibraryProvider {

    Optional<PromptLib> load(PromptRef promptRef);

}
