package io.github.codingspeedup.tags.ai.boundary;

import java.nio.file.Path;

public interface BufferProvider {

    Path getPath();

    default String getName() {
        return getPath().getFileName().toString();
    }

    String getContent();

}
