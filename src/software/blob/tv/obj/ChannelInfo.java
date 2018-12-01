package software.blob.tv.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.blob.tv.Constants;
import software.blob.tv.builders.ScheduleBuilder;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a channel
 */
public class ChannelInfo {

    private static final String TAG = "ChannelInfo";

    public int number, copyChannel = -1, copyChannelOffset = 0;
    public File schedule;
    public Playlist shorts, bumps, commercials, stationIds;
    public String name, playlist;

    public ChannelInfo(File dir, JsonObject jo) {
        if (jo.has("CopyChannel"))
            copyChannel = jo.get("CopyChannel").getAsInt();
        if (jo.has("CopyChannelOffset"))
            copyChannelOffset = jo.get("CopyChannelOffset").getAsInt();
        if (jo.has("Schedule")) {
            String schedName = jo.get("Schedule").getAsString();
            if (schedName.endsWith(".js")) {
                // Check for today's custom schedule
                String date = (new SimpleDateFormat("[MM-dd-yyyy]"))
                        .format(System.currentTimeMillis());
                schedule = new File(dir, schedName.replace(".js", " " + date + ".js"));
                if (!schedule.exists())
                    schedule = new File(dir, schedName);
                else
                    Log.d(TAG, "Using custom schedule for " + date);
            } else {
                File showDir = ScheduleBuilder.findShow(schedName);
                if (showDir.exists() && showDir.isDirectory()) {
                    // Only this show, all day!
                    schedule = showDir;
                }
            }
        }
        if (jo.has("Playlist"))
            playlist = jo.get("Playlist").getAsString();
        if (jo.has("Number"))
            number = jo.get("Number").getAsInt();
        if (jo.has("Name"))
            name = jo.get("Name").getAsString();
        if (jo.has("Playlist"))
            playlist = jo.get("Playlist").getAsString();
        if (jo.has("Number"))
            number = jo.get("Number").getAsInt();
        if (jo.has("Name"))
            name = jo.get("Name").getAsString();

        // Short films to play between shows
        if (jo.has("Shorts"))
            shorts = loadDirectory(jo.get("Shorts"));

        // Schedule bumpers
        if (jo.has("Bumpers"))
            bumps = loadDirectory(jo.get("Bumpers"));

        // Commercials
        if (jo.has("Commercials"))
            commercials = loadDirectory(jo.get("Commercials"));

        // Station IDs
        if (jo.has("IDs"))
            stationIds = loadDirectory(jo.get("IDs"));

        // Everything else is client-side for now
    }

    /**
     * Load a single or array of directory names into playlists
     * @param dirEl Directory JSON element (a string or array of strings)
     * @return A single playlist containing all the segments
     */
    private static Playlist loadDirectory(JsonElement dirEl) {
        Playlist ret;
        if (dirEl instanceof JsonArray) {
            ret = new Playlist();
            JsonArray arr = dirEl.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++)
                ret.addAll(new Playlist(arr.get(i).getAsString()));
        } else
            ret = new Playlist(dirEl.getAsString());
        ret.sortByDuration();
        return ret;
    }

    /**
     * Parse the complete channel info list
     * @param channelsFile Channels info file (channels.js)
     * @return Array of channel info
     */
    public static ChannelInfo[] parseChannelList(File channelsFile) {
        List<ChannelInfo> ret = new ArrayList<ChannelInfo>();
        if(FileUtils.readableFile(channelsFile, true)) {
            JsonArray channels = FileUtils.loadJSON(channelsFile).getAsJsonArray();
            for(JsonElement c : channels) {
                if(c.isJsonObject())
                    ret.add(new ChannelInfo(channelsFile.getParentFile(),
                            c.getAsJsonObject()));
            }
        }
        return ret.toArray(new ChannelInfo[0]);
    }
}
