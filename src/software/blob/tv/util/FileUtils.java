package software.blob.tv.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Common utilities for file checking and loading
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    public static boolean readableFile(File f, boolean complain) {
        if(!f.exists()) {
            if(complain)
                Log.e(TAG, f + " does not exist.");
            return false;
        } else if(!f.isFile()) {
            if(complain)
                Log.e(TAG, f + " is not a file.");
            return false;
        } else if(!f.canRead()) {
            if (complain)
                Log.e(TAG, f + " cannot be read.");
            return false;
        }
        return true;
    }

    public static boolean readableFile(File f) {
        return readableFile(f, false);
    }

    public static boolean readableDir(File dir, boolean complain) {
        if(!dir.exists()) {
            if(complain)
                Log.e(TAG, dir + " does not exist.");
            return false;
        } else if(!dir.isDirectory()) {
            if(complain)
                Log.e(TAG, dir + " is not a directory.");
            return false;
        } else if(!dir.canRead()) {
            if (complain)
                Log.e(TAG, dir + " cannot be read.");
            return false;
        }
        return true;
    }

    public static JsonElement loadJSON(File jsonFile) {
        JsonObject jo = new JsonObject();
        if(readableFile(jsonFile, true)) {
            try (FileReader fr = new FileReader(jsonFile)) {
                JsonStreamParser jsp = new JsonStreamParser(fr);
                if (jsp.hasNext())
                    return jsp.next();
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JSON file " + jsonFile, e);
            }
        }
        return jo;
    }

    public static boolean writeToFile(File output, String content) {
        try (FileWriter fw = new FileWriter(output)) {
            fw.write(content);
        } catch (Exception e) {
            Log.e(TAG, "Error writing to file: " + output, e);
            return false;
        }
        return true;
    }
}
