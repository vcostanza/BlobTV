package software.blob.tv.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.blob.tv.obj.Segment;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

/**
 * Holder for info.js, specific to a show
 */
public class ShowInfo {

    private static final String TAG = "ShowInfo";

    public static final String RUNTIME = "Runtime";
    public static final String SEPARATED = "SeparatedEps";
    public static final String RANDOMIZED = "Randomized";
    public static final String INTRO = "Intro";
    public static final String CREDITS = "Credits";
    public static final String BREAKS = "Breaks";
    public static final String EPISODES = "Episodes";

    protected JsonObject _js;
    public String showDir = "";
    //public int runTime = 30;
    public boolean randomized = false,
            separatedEps = false;
    public File introVid, creditsVid;
    public String[][] episodes;
    public Map<String, Break> breaks;

    public ShowInfo(File infoFile) {
        showDir = infoFile.getParent();
        _js = FileUtils.loadJSON(infoFile).getAsJsonObject();
        /*if(_js.has(RUNTIME))
            runTime = _js.get(RUNTIME).getAsInt();*/
        if(_js.has(SEPARATED))
            separatedEps = _js.get(SEPARATED).getAsBoolean();
        if(_js.has(RANDOMIZED))
            randomized = _js.get(RANDOMIZED).getAsBoolean();
        if(_js.has(INTRO))
            introVid = new File(showDir, _js.get(INTRO).getAsString());
        if(_js.has(CREDITS))
            creditsVid = new File(showDir, _js.get(CREDITS).getAsString());

        // Episode breaks
        if(_js.has(BREAKS) && _js.get(BREAKS).isJsonObject()) {
            JsonObject jo = _js.getAsJsonObject(BREAKS);
            breaks = new HashMap<>();
            for(Entry<String, JsonElement> entry : jo.entrySet()) {
                JsonElement je = entry.getValue();
                if(je.isJsonObject())
                    breaks.put(entry.getKey(), new Break(je.getAsJsonObject()));
            }
        }

        // Episode sets
        if(_js.has(EPISODES) && _js.get(EPISODES).isJsonArray()) {
            JsonArray epSets = _js.getAsJsonArray(EPISODES);
            episodes = new String[epSets.size()][];
            for(int i = 0; i < epSets.size(); i++) {
                JsonElement el = epSets.get(i);
                if(el.isJsonArray()) {
                    JsonArray set = el.getAsJsonArray();
                    episodes[i] = new String[set.size()];
                    for(int j = 0; j < set.size(); j++)
                        episodes[i][j] = set.get(j).getAsString();
                }
            }
        }
    }

    public String getShowDir() {
        return showDir;
    }

    /*public double getRuntimeSecs() {
        return runTime * 60.0;
    }*/

    public static class Break extends HashMap<String, Double> {

        public static final String INTRO = "intro";
        public static final String CREDITS = "credits";
        public static final String END = "end";
        public static final String EP_A = "episode_a";
        public static final String EP_B = "episode_b";
        public static final String EP_C = "episode_c";
        public static final String EP_D = "episode_d";
        public static final String EP_E = "episode_e";
        public static final String HAS_INTRO = "HasIntro";
        public static final String HAS_CREDITS = "HasCredits";

        private static final String[] TYPES = {
                INTRO, EP_A, EP_B, EP_C, EP_D, EP_E, CREDITS, END
        };

        private static final String[] MIDS = {
                EP_B, EP_C, EP_D, EP_E
        };

        public boolean hasIntro, hasCredits = false;

        public Break(JsonObject breakjs) {
            for(String s : TYPES) {
                if(breakjs.has(s)) {
                    try {
                        put(s, breakjs.get(s).getAsDouble());
                    } catch(Exception e) {
                        Log.e(TAG, "Failed to parse break in ", e);
                    }
                }
            }
            if (breakjs.has(HAS_INTRO))
                hasIntro = breakjs.get(HAS_INTRO).getAsBoolean();
            if (breakjs.has(HAS_CREDITS))
                hasCredits = breakjs.get(HAS_CREDITS).getAsBoolean();
        }

        public boolean has(String key) {
            return containsKey(key);
        }

        public void setDuration(Segment seg) {
            if(has(INTRO))
                seg.start = get(INTRO);
            if(has(END))
                seg.end = get(END);
        }

        public void getMidBreaks(Playlist pl, Segment seg) {
            seg.midBreaks = new double[0];
            int maxBreaks = pl.getSlotSize() / 15;
            if (maxBreaks <= 1)
                return;
            double segDur = seg.getDuration() / maxBreaks;
            double targetTime = segDur;
            List<Double> breaks = new ArrayList<Double>();
            List<String> mids = new ArrayList<String>();
            for(String str : MIDS) {
                if(has(str))
                    mids.add(str);
            }
            for (int i = 1; i <= maxBreaks - 1; i++) {
                double midBreak = -1;
                String breakName = null;
                for (String str : mids) {
                    double breakTime = get(str);
                    double diff = Math.abs(targetTime - breakTime);
                    if (diff < Math.abs(targetTime - midBreak)) {
                        midBreak = breakTime;
                        breakName = str;
                    }
                }
                if (midBreak != -1) {
                    breaks.add(midBreak);
                    mids.remove(breakName);
                }
                targetTime += segDur;
            }
            Collections.sort(breaks, new Comparator<Double>() {
                @Override
                public int compare(Double d1, Double d2) {
                    return Double.compare(d1, d2);
                }
            });
            seg.midBreaks = new double[breaks.size()];
            for (int i = 0; i < breaks.size(); i++)
                seg.midBreaks[i] = breaks.get(i);
        }
    }
}
