package io.github.codingspeedup.tags.engine.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerationResponse {

    private GenerationSink outputChannel;
    private String bufferName;
    private String generatedContent;
    private int startOffset;
    private int endOffset;

}
