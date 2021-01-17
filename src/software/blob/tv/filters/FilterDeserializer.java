package software.blob.tv.filters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.blob.tv.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles deserializing filters defined in JSON
 */
public class FilterDeserializer {

    private static final String TAG = "FilterDeserializer";

    // List of supported filters mapped by their type key
    private static final Map<String, Class<? extends SegmentFilter>> filterByType = new HashMap<>();
    static {
        registerFilter(new FadeFilter());
        registerFilter(new TextFilter());
    }

    // Gson deserializer instance(s)
    private static ThreadLocal<Gson> gson = new ThreadLocal<>();

    /**
     * Register a filter class by its name
     * @param filter Filter instance used for registration
     */
    public static void registerFilter(SegmentFilter filter) {
        filterByType.put(filter.type, filter.getClass());
    }

    /**
     * Get a single filter given a JSON object
     * @param obj JSON object
     * @return Filter or null if N/A
     */
    public static SegmentFilter getFilter(JsonObject obj) {
        try {
            // First we need the name to know what type of filter it is
            if (!obj.has("name")) {
                Log.e(TAG, "JSON filter metadata is missing name" + obj);
                return null;
            }
            String name = obj.get("name").getAsString();

            // Perform class lookup based on the name
            Class<? extends SegmentFilter> cl = filterByType.get(name);
            if (cl == null) {
                Log.e(TAG, "Failed to find filter with name \"" + name + "\"");
                return null;
            }

            // Deserialize filter
            Gson g = gson.get();
            if (g == null)
                gson.set(g = new Gson());
            return g.fromJson(obj, cl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert JSON object to filter", e);
        }
        return null;
    }

    /**
     * Get a list of filters given a JSON element
     * @param el JSON object or array
     * @return List of filters
     */
    public static List<SegmentFilter> getFilters(JsonElement el) {
        List<SegmentFilter> ret = new ArrayList<>();
        if (el == null)
            return ret;

        // A list of filters
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                if (!e.isJsonObject())
                    continue;
                SegmentFilter filter = getFilter(e.getAsJsonObject());
                if (filter != null)
                    ret.add(filter);
            }
        }

        // A single filter
        else if (el.isJsonObject()) {
            SegmentFilter filter = getFilter(el.getAsJsonObject());
            if (filter != null)
                ret.add(filter);
        }

        return ret;
    }
}
