package software.blob.tv.obj;

import com.google.gson.*;
import software.blob.tv.Config;
import software.blob.tv.filters.FilterDeserializer;
import software.blob.tv.filters.SegmentFilter;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;
import software.blob.tv.util.MathUtils;
import software.blob.tv.util.RandUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * Array of segments with emphasis on timing
 */
public class Playlist extends ArrayList<Segment> {

    private static final String TAG = "Playlist";
    public static final int MAX_SECS = 86400;

    private static final FileFilter MP4_FILTER = f -> f.isFile() && f.getName().endsWith(".mp4");

    private static final Comparator<Segment> DUR_COMPARATOR = Comparator.comparingDouble(Segment::getDuration);

    private static final Comparator<Segment> TIME_COMPARATOR = Comparator.comparingDouble(s -> s.startTime);

    private static final Map<File, Playlist> _showCache = new HashMap<>();

    private final Map<String, Object> _data = new HashMap<>();
    private boolean _sortedByDuration = false;
    private int _slotSize = 0;

    public Playlist() {
        super();
    }

    /**
     * Initialize playlist by copying from other playlist
     * @param other Playlist to copy from
     */
    public Playlist(Playlist other) {
        this();
        copy(other);
    }

    /**
     * Initialize playlist from directory
     * @param dir Directory of video files
     * @param slotSize Time slot size in minutes
     */
    public Playlist(File dir, int slotSize) {
        this(_showCache.get(dir));
        _slotSize = slotSize;

        if(!isEmpty() || !FileUtils.readableDir(dir, true))
            return;

        // Durations
        File durFile = new File(dir, Config.get("DUR_JS"));
        JsonObject durs = FileUtils.loadJSON(durFile).getAsJsonObject();

        // Stream start times (required by ffplay or everything is off)
        File startFile = new File(dir, Config.get("STARTS_JS"));
        JsonObject starts = FileUtils.loadJSON(startFile).getAsJsonObject();

        // Show information
        File infoFile = new File(dir, Config.get("INFO_JS"));
        ShowInfo info = null;
        if(infoFile.exists())
            info = new ShowInfo(infoFile);

        // Video filters
        File filtersFile = new File(dir, Config.get("FILTERS_JS"));
        List<SegmentFilter> filters = null;
        if (filtersFile.exists())
            filters = FilterDeserializer.getFilters(FileUtils.loadJSON(filtersFile));

        Playlist cached = new Playlist();
        cached.makeSegments(dir.listFiles(MP4_FILTER), starts, durs, info, filters);
        _showCache.put(dir, cached);
        copy(cached);
    }

    public Playlist(File dir) {
        this(dir, 30);
    }

    public Playlist(String showDir) {
        this(Config.getFile("SHOW_DIR", showDir));
    }

    public Playlist(Playlist segs, String[] episodes) {
        if (episodes != null) {
            for (String name : episodes) {
                Segment ep = segs.findByName(name);
                if (ep != null)
                    add(0.0, ep);
                else
                    Log.e(TAG, "Failed to find episode: " + name);
            }
        }
    }

    @Override
    public boolean add(Segment s) {
        _sortedByDuration = false;
        return super.add(s);
    }

    public void add(double startTime, Segment seg, boolean push) {
        if(seg == null)
            return;

        _sortedByDuration = false;

        if (seg.midBreaks != null && seg.midBreaks.length > 0) {
            // Split segments with mid-breaks
            double start = seg.start;
            for (int m = 0; m <= seg.midBreaks.length; m++) {
                double midBreak = m < seg.midBreaks.length ?
                        seg.midBreaks[m] : seg.end;
                if (midBreak > seg.start && midBreak <= seg.end) {
                    Segment part = new Segment(seg);
                    part.midBreaks = new double[0];
                    part.start = start;
                    part.end = start = midBreak;
                    add(startTime, part, push);
                }
            }
            return;
        }

        // Always copy before adding
        seg = new Segment(seg);

        int pos = 0;
        if(push) {
            double minDiff = Double.MAX_VALUE;
            for (int i = 0; i < size(); i++) {
                Segment s = get(i);
                double diff_s = Math.abs(startTime - s.startTime);
                double diff_e = Math.abs(startTime - s.endTime);
                if (diff_s < minDiff) {
                    seg.setStartTime(s.startTime);
                    pos = i;
                    minDiff = diff_s;
                }
                if (diff_e < minDiff) {
                    seg.setStartTime(s.endTime);
                    pos = i + 1;
                    minDiff = diff_e;
                }
            }
        } else {
            seg.setStartTime(startTime);
            for (Segment s : this) {
                if(seg.endTime <= s.startTime)
                    break;
                else if(seg.startTime <= s.startTime)
                    seg.setStartTime(s.endTime);
                pos++;
            }
            if(size() > 0 && pos >= size() && startTime < get(size()-1).endTime)
                seg.setStartTime(get(size()-1).endTime);
        }

        super.add(pos, seg);
        // Remove overlaps
        for(int i = pos+1; i < size(); i++) {
            if(get(i-1).endTime > get(i).startTime)
                get(i).setStartTime(get(i-1).endTime);
        }

        /*double[] gaps = getMaxGaps();
        if(gaps[0] != 0 && gaps[1] != 0) {
            Log.d(TAG, "bla");
        }*/
    }

    public void add(double startTime, Segment seg) {
        add(startTime, seg, false);
    }

    public void add(int startMin, Segment seg) {
        add(startMin * 60.0, seg);
    }

    public Segment last() {
        return get(size() - 1);
    }

    /**
     * Return the largest and smallest gap between two segments in the playlist
     * @return Smallest and largest gap in seconds
     */
    public double[] getMaxGaps() {
        if(size() <= 1)
            return new double[] {0, 0};
        double[] minMax = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
        for(int i = 0; i < size()-1; i++) {
            Segment s = get(i);
            Segment n = get(i+1);
            double gap = n.startTime - s.endTime;
            if (gap < 0)
                Log.e(TAG, "Negative gap between " + s.name + " and " + n.name);
            minMax[0] = Math.min(minMax[0], gap);
            minMax[1] = Math.max(minMax[1], gap);
        }
        return minMax;
    }

    @Override
    public Segment get(int index) {
        index = index % size() + (index < 0 ? size() : 0);
        return super.get(index);
    }

    /**
     * Merge this playlist with another
     * @param other Other playlist
     * @param simpleMethod True to push segments onto the stack
     */
    public void merge(Playlist other, boolean simpleMethod) {
        this.sort(TIME_COMPARATOR);
        for(Segment s : other) {
            if(simpleMethod)
                add(s);
            else
                add(s.startTime, s);
        }
        this.sort(TIME_COMPARATOR);
    }

    /**
     * Copy contents of other playlist to source
     * @param other Existing playlist
     */
    public void copy(Playlist other) {
        clear();
        if(other != null) {
            addAll(other);
            this._sortedByDuration = other._sortedByDuration;
            this._slotSize = other._slotSize;
        }
    }

    /**
     * Convert video files to segments (non-organized Playlist)
     * @param vids List of video files
     * @param starts Stream starts JSON data
     * @param durs Durations JSON data
     * @param info Info JSON data
     */
    public void makeSegments(File[] vids, JsonObject starts, JsonObject durs, ShowInfo info, List<SegmentFilter> filters) {
        clear();
        if (vids == null)
            return;
        for(File f : vids) {
            Segment seg = new Segment(f);
            String name = seg.name;
            if(!durs.has(name)) {
                Log.w(TAG, name + " is missing duration!");
                continue;
            }
            seg.streamStart = starts.has(name) ? starts.get(name).getAsDouble() : 0;
            seg.start = 0.0;
            seg.end = durs.get(name).getAsDouble();
            if (filters != null)
                seg.filters = new ArrayList<>(filters);

            // Use start/end provided by info metadata
            if(info != null && info.breaks != null && info.breaks.containsKey(name)) {
                ShowInfo.Break br = info.breaks.get(name);
                br.setDuration(seg);
            }
            add(seg);
        }
    }

    /**
     * Find segment by path within Playlist
     * @param path Path to video file
     * @return Segment within playlist with matching path
     */
    public Segment findByPath(String path) {
        for(Segment s : this) {
            if(path.equals(s.path))
                return s;
        }
        return null;
    }

    /**
     * Find segment by name
     * @param name Name of segment
     * @return Matching segment or null if not found
     */
    public Segment findByName(String name) {
        for (Segment s : this) {
            if (name.equals(s.name))
                return s;
        }
        return null;
    }

    /**
     * Find segment by format (SHOW, COMMERCIAL, STATION_ID, etc.)
     * @param format Segment format
     * @return Matching segment or null if not found
     */
    public Segment findByFormat(Segment.Format format) {
        for (Segment s : this) {
            if (format == s.format)
                return s;
        }
        return null;
    }

    /**
     * Find all segments by format
     * @param format Segment format
     * @return List of matching segments
     */
    public List<Segment> findAllByFormat(Segment.Format format) {
        List<Segment> ret = new ArrayList<>();
        for (Segment s : this) {
            if (s.format == format)
                ret.add(s);
        }
        return ret;
    }

    /**
     * Move playlist over by number of seconds
     * @param seconds Seconds to offset beginning of playlist
     */
    public void timeShift(double seconds) {
        for(Segment s : this) {
            s.startTime = MathUtils.modRange(s.startTime + seconds, MAX_SECS);
            s.endTime = MathUtils.modRange(s.endTime + seconds, MAX_SECS);
        }
        this.sort(TIME_COMPARATOR);
    }

    /**
     * Move playlist over by a number of minutes
     * @param mins Minutes to offset beginning of playlist
     */
    public void timeShift(int mins) {
        timeShift(mins * 60.0d);
    }

    /**
     * Exactly what it says
     */
    public void sortByDuration() {
        if(_sortedByDuration)
            return;
        this.sort(DUR_COMPARATOR);
        _sortedByDuration = true;
    }

    /**
     * Get random segment from playlist
     * @return Random segment
     */
    public Segment getRandomSegment() {
        if(size() > 0)
            return get(RandUtils.rand(size()));
        return null;
    }

    /**
     * Get random segment equal to under a duration
     * @param maxDuration Maximum duration of segment
     * @return Random segment
     */
    public Segment getRandomSegment(double maxDuration) {
        sortByDuration();
        int limit = 0;
        for (Segment s : this) {
            if(s.getDuration() > maxDuration)
                break;
            limit++;
        }
        if(limit > 0)
            return get(RandUtils.rand(limit));
        return null;
    }

    /**
     * Get random segment that's between min and max duration
     * @param minDuration Minimum duration of segment
     * @param maxDuration Maximum duration of segment
     * @return Random segment
     */
    public Segment getRandomSegment(double minDuration, double maxDuration) {
        sortByDuration();
        int start = 0, limit = 0;
        for (Segment s : this) {
            if(s.getDuration() < minDuration)
                start++;
            if(s.getDuration() > maxDuration)
                break;
            limit++;
        }
        limit -= start;
        if(limit > 0)
            return get(start + RandUtils.rand(limit));
        return null;
    }

    /**
     * Cumulative duration of segments
     * @return Total duration in seconds
     */
    public double getDuration() {
        double dur = 0;
        for(Segment s : this)
            dur += s.getDuration();
        return dur;
    }

    /**
     * Stretch playlist to duration by inserting time gaps between each segment
     * @param duration Duration in seconds
     */
    public void stretchDuration(double duration) {
        if(isEmpty())
            return;
        double curDur = getDuration();
        double gapSize = (duration - curDur) / size();
        double gapTotal = 0;
        for(Segment s : this) {
            s.setStartTime(s.startTime + gapTotal);
            gapTotal += gapSize;
        }
    }

    public void setSlotSize(int mins) {
        _slotSize = mins;
    }

    /**
     * Return the desired time slot size of this playlist
     * @return Slot size in minutes
     */
    public int getSlotSize() {
        return _slotSize;
    }

    public double getSlotSizeSecs() {
        return _slotSize * 60.0d;
    }

    public double getDeadAir() {
        return getSlotSizeSecs() - getDuration();
    }

    /**
     * Get number of possible breaks in this playlist
     * @return Number of breaks in the playlist
     */
    public int getNumBreaks() {
        int numBreaks = 0;
        for (Segment s : this) {
            if (s.format == Segment.Format.SHOW)
                numBreaks++;
        }
        return Math.min(numBreaks, getSlotSize() / 15);
    }

    /**
     * Get the best segments to insert breaks after
     * @return Playlist of segments which breaks should go after
     */
    public Playlist getBreakSegs() {
        int numBreaks = getNumBreaks();

        // Find the best slots to insert commercials
        double duration = getDuration();
        Playlist slots = new Playlist();
        Segment schedBump = findByFormat(Segment.Format.SCHED_BUMPER);
        List<Segment> bumpers = findAllByFormat(Segment.Format.BREAK_BUMPER);
        if (schedBump != null)
            bumpers.add(schedBump);
        // Already taken care of
        if (bumpers.size() >= numBreaks) {
            slots.addAll(bumpers);
            return slots;
        }
        for (int i = 0; i < numBreaks; i++) {
            double target = (duration / numBreaks) * (i + 1);
            double minDiff = Double.MAX_VALUE;
            boolean last = i == numBreaks - 1;
            Segment match = null;
            if (last && schedBump != null)
                match = schedBump;
            else {
                for (Segment s : this) {
                    if (s.format == Segment.Format.SHOW && !slots.contains(s)
                            && (schedBump == null || Double.compare(
                            s.endTime, schedBump.startTime) != 0)) {
                        double diff = Math.abs(s.endTime - target);
                        if (diff < minDiff) {
                            minDiff = diff;
                            match = s;
                        }
                    }
                }
            }
            if (match != null)
                slots.add(match);
        }
        return slots;
    }

    public void setMetaString(String key, String value) {
        _data.put(key, value);
    }

    public String getMetaString(String key, String defValue) {
        return (String) getMetaValue(key, defValue, String.class);
    }

    public void setMetaStringArray(String key, String[] values) {
        _data.put(key, values);
    }

    public String[] getMetaStringArray(String key, String[] defValues) {
        return (String[]) getMetaValue(key, defValues, String[].class);
    }

    public boolean hasMetaValue(String key) {
        return _data.containsKey(key);
    }

    private Object getMetaValue(String key, Object def, Class<?> c) {
        Object ret = _data.get(key);
        if (!(c.isInstance(ret)))
            return def;
        return c.cast(ret);
    }

    /**
     * Convert playlist to a human-readable JSON string
     * Also strips absolute show directory to reduce size
     * @return Playlist as JSON string
     */
    public String toJsonString() {
        String homeDir = Config.get("BTV_HOME") + File.separator;
        GsonBuilder gs = new GsonBuilder();
        gs.disableHtmlEscaping(); // No need to HTML-escape special characters
        return gs.create().toJson(this)
                .replaceAll("},", "},\n")
                .replaceAll(homeDir, "");
    }
}
