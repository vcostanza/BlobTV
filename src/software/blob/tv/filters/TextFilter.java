package software.blob.tv.filters;

import software.blob.tv.util.LineWrap;
import software.blob.tv.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A filter for rendering text on a video segment
 *
 * Note: Like {@link PositionFilter}, the size parameters here are relative to a 480p frame.
 */
public class TextFilter extends PositionFilter {

    private static final String TAG = "TextFilter";

    // Text alignment flags
    public static final int ALIGN_LEFT = 1,
            ALIGN_CENTER_X = 1 << 1,
            ALIGN_RIGHT = 1 << 2,
            ALIGN_TOP = 1 << 3,
            ALIGN_CENTER_Y = 1 << 4,
            ALIGN_BOTTOM = 1 << 5;

    // The text to display
    public String text;

    // The name of the font to use for this text (null to use default)
    public String font;

    // The size of the font (480p pixels)
    public int size;

    // The font size after each line wrap (overrides size field)
    public int[] sizes;

    // Line wrapping (character count)
    public Integer wrap;

    // Line wrapping after each line wrap (overrides wrap field)
    public int[] wraps;

    // Text alignment (top-left by default)
    public int align = ALIGN_LEFT | ALIGN_BOTTOM;

    // Text color in hexadecimal (null = black)
    public String color;

    // Text border color (null = black)
    public String borderColor;

    // Text border thickness (0 = no border)
    public Integer borderWidth;

    // Fade in start and end timestamps
    public float[] fadeIn;

    // Fade out start and end timestamps
    public float[] fadeOut;

    public TextFilter() {
        super("text");
    }

    @Override
    public TextFilter copy(SegmentFilter copy) {
        TextFilter ret = copy instanceof TextFilter ? (TextFilter) copy : new TextFilter();
        super.copy(ret);
        ret.text = this.text;
        ret.align = this.align;
        ret.font = this.font;
        ret.size = this.size;
        ret.sizes = this.sizes != null ? Arrays.copyOf(this.sizes, 2) : null;
        ret.wrap = this.wrap;
        ret.wraps = this.wraps != null ? Arrays.copyOf(this.wraps, 2) : null;
        ret.color = this.color;
        ret.borderColor = this.borderColor;
        ret.borderWidth = this.borderWidth;
        ret.fadeIn = Arrays.copyOf(this.fadeIn, 2);
        ret.fadeOut = Arrays.copyOf(this.fadeOut, 2);
        return ret;
    }

    /**
     * Replace a variable in the text
     * @param varName Variable name (not including %)
     * @param set String to replace the variable with
     */
    public void setVariable(String varName, String set) {
        text = text.replace("%" + varName + "%", set);
    }

    /**
     * Parses the text content of this filter for numbered variables
     * i.e. "%show1%" = 1
     * @param varName The variable name to search for (not including %)
     * @return Variable number or -1 if not found
     */
    public int getVariableNum(String varName) {
        int varIdx = text.indexOf("%" + varName);
        if (varIdx == -1)
            return -1;
        int lastPcnt = text.indexOf('%', varIdx + 1);
        if (lastPcnt == -1)
            return -1;
        String value = text.substring(varIdx + varName.length() + 1, lastPcnt);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse variable number for \"" + varName + "\": " + value, e);
        }
        return -1;
    }

    /**
     * Wrap the lines of text in this filter by inserting appropriate line breaks
     * @return New text filters making up each line
     */
    public List<TextFilter> wrapLines() {
        // Can't wrap without a set value
        if (this.wrap == null && this.wraps == null)
            return Collections.singletonList(this);

        // Get the initial wrap length and size
        int wrap = this.wrap != null ? this.wrap : this.wraps[0];
        int size = this.size;

        // Check if we need to increase the wrap length and decrease font size
        if (this.sizes != null && this.wraps != null) {
            size = this.sizes[0];
            for (int w = 0; w < this.sizes.length - 1; w++) {
                // Check if the text wraps at this threshold
                if (LineWrap.getWrapIndex(text, wraps[w], 0) < text.length()) {
                    // If it does, increase the wrap length and decrease text size
                    wrap = this.wraps[w + 1];
                    size = this.sizes[w + 1];
                }
            }
        }

        // Perform the line wrap with the best fit value
        List<String> lines = LineWrap.wrapLines(this.text, wrap);

        // Create the copies
        List<TextFilter> ret = new ArrayList<>(lines.size());
        for (int l = 0; l < lines.size(); l++) {
            String line = lines.get(l);
            TextFilter tf = this.copy(null);

            // Update values for lines
            tf.text = line;
            tf.size = size;
            tf.y += (tf.size * 1.25) * l;

            // Remove semi-transient values that the player doesn't need
            this.wrap = null;
            this.wraps = this.sizes = null;

            ret.add(tf);
        }

        return ret;
    }

    @Override
    public String toString() {
        return text + " (size " + size + ")";
    }
}
