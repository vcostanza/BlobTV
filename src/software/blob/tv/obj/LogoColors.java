package software.blob.tv.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 2-color combination for all shows
 */
public class LogoColors {

    private static final String TAG = "LogoColors";
    public static final Color[] DEFAULT_COLORS = new Color[] {
            Color.decode("#FFE000"),
            Color.decode("#00FFFF")
    };

    public static LogoColors load(File colorsFile) {
        if (colorsFile.exists()) {
            try {
                return new LogoColors(FileUtils.loadJSON(colorsFile));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse schedule file", e);
            }
        }
        return null;
    }

    private Map<String, Color[]> _colors;

    public LogoColors(JsonElement colorsJs) {
        _colors = new HashMap<>();
        if (colorsJs != null) {
            JsonObject jo = colorsJs.getAsJsonObject();
            for (Map.Entry<String, JsonElement> map : jo.entrySet()) {
                JsonArray colArr = map.getValue().getAsJsonArray();
                Color[] colors = new Color[colArr.size()];
                for (int i = 0; i < colors.length; i++) {
                    String hexStr = colArr.get(i).getAsString();
                    colors[i] = Color.decode(hexStr);
                }
                _colors.put(map.getKey(), colors);
            }
        }
    }

    /**
     * Get the show color palette
     * @param showName Name of show
     * @return Array of colors
     */
    public Color[] getShowColors(String showName) {
        if (_colors.containsKey(showName))
            return _colors.get(showName);
        return DEFAULT_COLORS;
    }
}
