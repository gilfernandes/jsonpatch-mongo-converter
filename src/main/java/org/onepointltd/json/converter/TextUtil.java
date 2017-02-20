package org.onepointltd.json.converter;

/**
 * Simple utility with common text related methods.
 */
class TextUtil {

    private TextUtil() {
    }

    static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
