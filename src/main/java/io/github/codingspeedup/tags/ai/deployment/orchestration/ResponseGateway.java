package io.github.codingspeedup.tags.ai.deployment.orchestration;

import org.apache.commons.lang.StringUtils;

import static io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel.SECTION_REF_MARKER;

public enum ResponseGateway {

    CHAT,
    CLIPBOARD,
    CONTENT,

    INFO,
    WARN,
    ERROR,

    IGNORE

    ;

    public static ResponseGateway resolveGateway(String gateway) {
        gateway = StringUtils.trimToEmpty(gateway);
        if (gateway.startsWith(SECTION_REF_MARKER)) {
            return ResponseGateway.CONTENT;
        }
        if (StringUtils.equalsIgnoreCase(ResponseGateway.CLIPBOARD.name(), gateway)) {
            return ResponseGateway.CLIPBOARD;
        }
        return ResponseGateway.CHAT;
    }

}
