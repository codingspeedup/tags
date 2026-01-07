package io.github.codingspeedup.tags.prompting.template;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class PromptBlock {

    private int fromOffset;
    private int toOffset = -1;

    public boolean contains(int offset) {
        if (offset < fromOffset) {
            return false;
        }
        return toOffset < 0 || offset <= toOffset;
    }
}
