package io.github.codingspeedup.tags.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagsResult {

    private final ActionResultGateway gateway;
    private String bufferName;
    private String content;
    private int startOffset;
    private int endOffset;

    public TagsResult(ActionResultGateway gateway) {
        this.gateway = gateway;
    }

}
