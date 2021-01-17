package software.blob.tv.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Methods for breaking a line of text into multiple lines based on a max character count per line
 */
public class LineWrap {

    /**
     * Calculate the number of lines needed to perform a line wrap on some text
     * @param text Text to scan for wrap
     * @param wrap The maximum number of characters allowed before wrapping
     * @param start The index to begin scanning text
     * @return Index where the line break should be inserted
     */
    public static int getWrapIndex(String text, int wrap, int start) {
        int j = 0;
        int ld = -1;
        for (int i = start; i < text.length() && j <= wrap; i++) {
            if (Character.isWhitespace(text.charAt(i)))
                ld = i;
            j++;
        }
        if (j <= wrap)
            ld = text.length();
        return ld;
    }

    /**
     * Wrap text into multiple lines (if necessray)
     * @param text Text to wrap
     * @param wrap The maximum number of characters allowed before wrapping
     * @return List containing string for each line
     */
    public static List<String> wrapLines(String text, int wrap) {
        List<String> lines = new ArrayList<>();
        int ld = 0;
        int start = 0;
        int strLen = text.length();
        while (ld < strLen) {
            ld = getWrapIndex(text, wrap, start);
            lines.add(text.substring(start, ld));
            start = ld + 1;
        }
        return lines;
    }
}
