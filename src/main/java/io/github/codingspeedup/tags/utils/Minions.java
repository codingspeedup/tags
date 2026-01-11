package io.github.codingspeedup.tags.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Minions {

    public static String sanitizeLineEndings(String message) {
        if (message == null) {
            return StringUtils.EMPTY;
        }
        return message.replaceAll("\\r\\n|\\r", "\n");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean endsWith(StringBuilder sb, String text) {
        if (sb == null) {
            return text == null;
        }
        if (text == null) {
            return false;
        }
        var textLen = text.length();
        if (textLen == 0) {
            return true;
        }
        var sbLen = sb.length();
        if (sbLen < textLen) {
            return false;
        }
        for (var i = 0; i < textLen; i++) {
            if (sb.charAt(sbLen - textLen + i) != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

}
