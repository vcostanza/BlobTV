package software.blob.tv.builders;

import software.blob.tv.*;
import software.blob.tv.obj.*;
import software.blob.tv.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Create 24-hour playlist in JSON
 */
public class ScheduleBuilder {

    private static final String TAG = "ScheduleBuilder";
    private static final int DAY_MINS = 1440;
    private static final File[] SHOWS;

    private final ChannelInfo _channel;
    private final Schedule _sched;
    private boolean _valid = false;

    static {
        // Get list of shows
        File showDir = Config.getFile("SHOW_DIR");
        SHOWS = showDir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
    }

    /**
     * Create new schedule builder
     * @param channel Information regarding the channel
     */
    public ScheduleBuilder(ChannelInfo channel, Schedule sched) {
        _channel = channel;
        _sched = sched;
        _valid = _channel.schedule.isFile();
        if(!_valid)
            Log.e(TAG, "Parameters are not valid: 'schedPath' must be a file");
    }

    public ScheduleBuilder(ChannelInfo channel) {
        this(channel, Schedule.load(channel.schedule));
    }

    public Playlist build() {
        Playlist playlist = new Playlist();
        if(!_valid) {
            Log.e(TAG, "Builder is not valid. Returning empty playlist...");
            return playlist;
        }
        if(_sched == null) {
            Log.e(TAG, _channel.schedule + " is not a valid schedule file.");
            return playlist;
        }
        for(int i = 0; i < _sched.size(); i++) {
            int n = (i == _sched.size() - 1 ? 0 : i + 1);
            int startTime = _sched.get(i).TimeSlot;
            int endTime = _sched.get(n).TimeSlot + (n == 0 ? DAY_MINS : 0);
            if(endTime < startTime) {
                Log.e(TAG, "Invalid slot between " + startTime + " and " + endTime);
                return playlist;
            }
            Playlist pl = processSlot(_sched.get(i), endTime, findShow(_sched.get(i).Show));
            playlist.merge(pl, true);

            /*double[] gaps = playlist.getMaxGaps();
            if(gaps[0] < 0 || gaps[1] > 10) {
                Log.d(TAG, "bla");
            }*/
        }
        return playlist;
    }

    /**
     * Find show directory by name
     * @param show Show name (without parentheses)
     * @return Show directory
     */
    public static File findShow(String show) {
        for(File dir : SHOWS) {
            if(dir.isDirectory() && dir.getName().equals(show)
                    || dir.getName().startsWith(show + " ("))
                return dir;
        }
        return Config.getFile("SHOW_DIR", show);
    }

    /**
     * Fill an entire day with segments from a single show
     * @param show Show directory
     * @return Full-day playlist
     */
    public static Playlist buildForShow(File show) {
        Playlist segsR = new Playlist(show);
        Playlist segsW = new Playlist(segsR);
        Playlist pl = new Playlist();
        double remTime = Playlist.MAX_SECS;
        while (true) {
            if (segsW.isEmpty())
                segsW = new Playlist(segsR);
            Segment s = segsW.getRandomSegment(remTime);
            if (s != null) {
                pl.add(Playlist.MAX_SECS - remTime, s);
                remTime -= s.getDuration();
                segsW.remove(s);
            } else
                break;
        }
        if (remTime > 0)
            pl.stretchDuration(Playlist.MAX_SECS);
        return pl;
    }

    private Playlist processSlot(ScheduleSlot slot, int endMin, File showDir) {
        // Determine eligible segments
        Playlist segs = new Playlist(showDir);
        String[] epNames = null;
        if (slot.Episode != null)
            epNames = new String[] { slot.Episode };
        else if (slot.Episodes != null)
            epNames = slot.Episodes.toArray(new String[slot.Episodes.size()]);
        if (epNames != null)
            segs.setMetaStringArray("episodes", epNames);

        // Set the slot size based on time difference
        segs.setSlotSize(endMin - slot.TimeSlot);

        // Show information
        ShowInfo info = new ShowInfo(new File(showDir, Config.get("INFO_JS")));

        // Read in breaks
        if (info.breaks != null) {
            for (Segment s : segs) {
                ShowInfo.Break br = info.breaks.get(s.name);
                if (br != null)
                    br.getMidBreaks(segs, s);
            }
        }

        // Verify - Debug only
        //verifySegs(info, segs);

        // Create schedule slot
        SlotBuilder sb = SlotBuilder.create(segs, info, _channel);
        Playlist pl = sb.build();
        pl.timeShift(slot.TimeSlot);
        return pl;
    }

    /**
     * Debugging tool
     * Verify all normal segments have breaks defined
     * @param info Show info
     * @param segs Segments playlist
     */
    private void verifySegs(ShowInfo info, Playlist segs) {
        if (info.breaks != null && segs.getSlotSize() > 15) {
            for (Segment s : segs) {
                boolean found = false;
                for (String k : info.breaks.keySet()) {
                    if (s.name.equals(k)) {
                        found = true;
                        break;
                    }
                }
                if (!found && s.epType == Segment.EpisodeType.NORMAL
                        && s.getDuration() < segs.getSlotSize())
                    Log.w(TAG, "Failed to find break for " + info.showDir + "/" + s.name);
            }
        }
    }
}
