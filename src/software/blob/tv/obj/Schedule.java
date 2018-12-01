package software.blob.tv.obj;

import com.google.gson.Gson;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;
import software.blob.tv.util.MathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * List of scheduled show time slots
 */
public class Schedule extends ArrayList<ScheduleSlot> {

    private static final String TAG = "Schedule";

    private static final Comparator<ScheduleSlot> TIME_COMPARATOR = new Comparator<ScheduleSlot>() {
        public int compare(ScheduleSlot s1, ScheduleSlot s2) {
            return Double.compare(s1.TimeSlot, s2.TimeSlot);
        }
    };

    public static Schedule load(File schedFile) {
        if (schedFile != null && schedFile.exists()) {
            try {
                return (new Gson()).fromJson(FileUtils.loadJSON(schedFile), Schedule.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse schedule file", e);
            }
        }
        return null;
    }

    /**
     * Create and return a copy of a schedule
     * @param other The schedule to copy
     * @param offsetMins Time to offset each slot in minutes
     * @return Schedule copy
     */
    public static Schedule copy(Schedule other, int offsetMins) {
        if (other != null) {
            Schedule copy = new Schedule();
            for (ScheduleSlot ss : other) {
                ScheduleSlot ss_copy = new ScheduleSlot(ss);
                ss_copy.TimeSlot = (int) MathUtils.modRange(ss_copy.TimeSlot + offsetMins, 1440);
                copy.add(ss_copy);
            }
            Collections.sort(copy, TIME_COMPARATOR);
            return copy;
        }
        return null;
    }

    /**
     * Read episodes from playlist into schedule
     * @param pl Matching schedule playlist
     */
    public void readEpisodes(Playlist pl) {
        for (ScheduleSlot ss : this)
            ss.Episodes = new ArrayList<String>();
        for (Segment s : pl) {
            ScheduleSlot match = findShow(s);
            if (match != null)
                match.addEpisode(s.name);
        }
    }

    /**
     * Get the corresponding schedule slot w/ wrapping
     * @param index Slot index
     * @param wrap True to wrap index to size
     * @return Matching slot
     */
    public ScheduleSlot get(int index, boolean wrap) {
        if (wrap)
            return get(index % size());
        return get(index);
    }

    /**
     * Get run time of show
     * @param index Index of show
     * @return Run time in minutes
     */
    public int getRunTime(int index) {
        if (index < 0 || index >= size())
            return 0;
        int next = index + 1;
        if (next == size())
            return 1440 - get(index).TimeSlot;
        return get(next).TimeSlot - get(index).TimeSlot;
    }

    public int getRunTime(ScheduleSlot ss) {
        int index = indexOf(ss);
        if (index != -1)
            return getRunTime(index);
        return 0;
    }

    /**
     * Find show slot matching segment
     * @param seg Segment to match
     * @return Show slot
     */
    public ScheduleSlot findShow(Segment seg) {
        if (seg.format == Segment.Format.SHOW) {
            int startMin = (int) Math.round(seg.startTime / 60);
            for (ScheduleSlot ss : this) {
                if (seg.show.equals(ss.Show) && startMin >= ss.TimeSlot
                        && startMin < ss.TimeSlot + getRunTime(ss)) {
                    return ss;
                }
            }
        }
        return null;
    }

    public ScheduleSlot findSlotByTime(int startMin) {
        for (int i = 0; i < size(); i++) {
            ScheduleSlot ss = get(i);
            if (startMin >= ss.TimeSlot && startMin < ss.TimeSlot + getRunTime(i))
                return ss;
        }
        return null;
    }

    /**
     * Convert schedule to a human-readable JSON string
     * @return Schedule as JSON string
     */
    public String toJsonString() {
        Gson gs = new Gson();
        return gs.toJson(this)
                .replaceAll("},", "},\n");
    }
}
