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
    public File schedule, shortsDir, bumpsDir;
    public Playlist shorts, bumps;
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
        if (jo.has("Shorts")) {
            shortsDir = new File(Constants.SHOW_DIR, jo.get("Shorts").getAsString());
            shorts = new Playlist(shortsDir);
        }
        if (jo.has("Bumpers")) {
            bumpsDir = new File(Constants.SHOW_DIR, jo.get("Bumpers").getAsString());
            bumps = new Playlist(bumpsDir);
        }
        // Everything else is client-side for now
    }

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
        return ret.toArray(new ChannelInfo[ret.size()]);
    }
}
