package software.blob.tv.util;

import java.security.SecureRandom;
import java.util.List;

public class RandUtils {

    private static SecureRandom _rand;
    private static long _randCount = 0;

    private static void checkRandom() {
        if(_rand == null || _randCount++ == 100) {
            _rand = new SecureRandom();
            _randCount = 0;
        }
    }

    /**
     * Return a random integer between 0 and max (exclusive)
     * @param max Max number
     * @return Random integer
     */
    public static int rand(int max) {
        checkRandom();
        return _rand.nextInt(max);
    }

    public static double rand(double max) {
        checkRandom();
        return (int) (_rand.nextDouble() * max);
    }

    public static Object randomItem(Object[] arr) {
        return arr[rand(arr.length)];
    }

    public static Object randomItem(List<Object> arr) {
        return arr.get(rand(arr.size()));
    }

    public static int randomIndex(Object[] arr) {
        return rand(arr.length);
    }

    public static int randomIndex(List<Object> arr) {
        return rand(arr.size());
    }

    public static Object getItem(Object[] arr, int index) {
        return arr[index % arr.length];
    }

    public static Object getItem(List<Object> arr, int index) {
        return arr.get(index % arr.size());
    }
}
