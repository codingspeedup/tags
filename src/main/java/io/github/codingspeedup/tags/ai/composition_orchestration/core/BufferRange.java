package io.github.codingspeedup.tags.ai.composition_orchestration.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BufferRange {

    private int fromOffset;
    private int toOffset = -1;

    public boolean contains(int offset) {
        if (offset < fromOffset) {
            return false;
        }
        return toOffset < 0 || offset < toOffset;
    }
}
