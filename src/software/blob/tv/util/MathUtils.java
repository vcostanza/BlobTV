package software.blob.tv.util;

/**
 * Math helper methods
 */
public class MathUtils {

    /**
     * Constrain a value to a wrapping range (such as degrees)
     * @param value The value to constrain/wrap
     * @param range The max value in the range
     * @return Constrained value
     */
    public static double modRange(double value, double range) {
        return (value % range) + (value < 0 ? range : 0);
    }
}
