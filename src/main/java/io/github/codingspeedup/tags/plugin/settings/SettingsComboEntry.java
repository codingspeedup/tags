package io.github.codingspeedup.tags.plugin.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

@AllArgsConstructor
@Getter
public class SettingsComboEntry {

    public static final SettingsComboEntry EMPTY_VALUE = new SettingsComboEntry(StringUtils.EMPTY, "-- None --");

    private final String code;
    private final String description;

    @Override
    public String toString() {
        return StringUtils.isBlank(description) ? code : description;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SettingsComboEntry that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

}
