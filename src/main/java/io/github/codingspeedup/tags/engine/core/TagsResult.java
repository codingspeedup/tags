package io.github.codingspeedup.tags.engine.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagsResult {

    private ActionResultGateway gateway;
    private String bufferName;
    private String content;
    private int startOffset;
    private int endOffset;

}
