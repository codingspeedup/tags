package io.github.codingspeedup.tags.ai.boundary;

import java.nio.file.Path;
import java.util.Optional;

public interface BufferProvider {

    Path getPath();

    default String getName() {
        var path = getPath();
        if (path != null) {
            return getPath().getFileName().toString();
        }
        return null;
    }

    String getContent();

    Optional<BufferProvider> resolve(String bufferRef);

}
