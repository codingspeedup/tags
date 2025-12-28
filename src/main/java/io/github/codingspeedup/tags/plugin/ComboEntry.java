package io.github.codingspeedup.tags.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

@AllArgsConstructor
@Getter
public class ComboEntry {

    public static final ComboEntry EMPTY_VALUE = new ComboEntry(StringUtils.EMPTY, "-- Default --");

    private final String code;
    private final String description;

    @Override
    public String toString() {
        return StringUtils.isBlank(description) ? code : description;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComboEntry that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

}
