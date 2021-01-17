package software.blob.tv.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;

/**
 * Android-like log class
 */
public class Log {

    public static void d(String tag, String msg, Throwable e) {
        log(tag, msg, e, "DEBUG");
    }

    public static void d(String tag, String msg) {
        d(tag, msg, null);
    }

    public static void w(String tag, String msg, Throwable e) {
        log(tag, msg, e, "WARNING");
    }

    public static void w(String tag, String msg) {
        w(tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable e) {
        log(tag, msg, e, "ERROR");
    }

    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }

    private static void log(String tag, String msg, Throwable e, String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        PrintStream stream = type.equals("ERROR") ? System.err : System.out;
        stream.println(sdf.format(System.currentTimeMillis()) + " / " + tag + " [" + type.charAt(0) + "]: " + msg);
        if(e != null)
            e.printStackTrace(stream);
    }
}
