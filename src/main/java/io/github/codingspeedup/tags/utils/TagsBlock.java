package io.github.codingspeedup.tags.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class TagsBlock {

    private int fromOffset;
    private int toOffset = -1;

    public boolean contains(int offset) {
        if (offset < fromOffset) {
            return false;
        }
        return toOffset < 0 || offset <= toOffset;
    }
}
