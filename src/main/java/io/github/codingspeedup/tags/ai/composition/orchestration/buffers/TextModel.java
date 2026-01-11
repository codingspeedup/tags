package io.github.codingspeedup.tags.ai.composition.orchestration.buffers;

import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import org.apache.commons.lang.StringUtils;

public class TextModel extends BufferModel {

    public TextModel() {
        this(StringUtils.EMPTY);
    }

    protected TextModel(String lineCommentPrefix) {
        super(
                lineCommentPrefix + "T: ",
                lineCommentPrefix + "A: ",
                lineCommentPrefix + "G: ",
                lineCommentPrefix + "S: ",
                lineCommentPrefix + "+: "
        );
    }

}
