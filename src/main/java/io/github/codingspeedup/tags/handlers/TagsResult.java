package io.github.codingspeedup.tags.handlers;

import io.github.codingspeedup.tags.ai.deployment_orchestration.ResponseGateway;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagsResult {

    private final ResponseGateway gateway;
    private String bufferName;
    private String content;
    private int startOffset;
    private int endOffset;

    public TagsResult(ResponseGateway gateway) {
        this.gateway = gateway;
    }

}
