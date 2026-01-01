package io.github.codingspeedup.tags.utils;

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
