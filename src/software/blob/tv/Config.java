package software.blob.tv;

import software.blob.tv.util.Log;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * User-defined file paths and names
 */
public class Config {

    private static final String TAG = "Config";

    private static final Map<String, String> _data = new HashMap<>();

    static void load(File config) {
        BufferedReader br = null;
        try {
            if (config.exists())
                // Read from local file
                br = new BufferedReader(new FileReader(config));
            else
                // Attempt to read from JAR
                br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Config.class.getClassLoader()
                        .getResourceAsStream(config.getName()))));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                int split = line.indexOf(" = ");
                if (split == -1)
                    continue;
                String key = line.substring(0, split);
                String value = line.substring(split + 3);
                if (value.contains("$")) {
                    for (Map.Entry<String, String> e : _data.entrySet())
                        value = value.replace("$" + e.getKey(), e.getValue());
                }
                _data.put(key, value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read config " + config, e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception ignore) {}
        }
    }

    public static String get(String key) {
        return _data.get(key);
    }

    public static File getFile(String key) {
        return new File(get(key));
    }

    public static File getFile(String key, String fileName) {
        return new File(get(key), fileName);
    }
}
