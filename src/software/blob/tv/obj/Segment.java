package software.blob.tv.obj;

import software.blob.tv.Config;
import software.blob.tv.util.Log;

import java.io.File;

/**
 * Video segment (episode, commercial, interstitial, bumper)
 */
public class Segment implements Comparable<Segment> {

    private static final String TAG = "Segment";

    public enum Format {
        SHOW, COMMERCIAL, SHORT, BREAK_BUMPER, SCHED_BUMPER, STATION_ID;

        @Override
        public String toString() {
            switch(this) {
                case COMMERCIAL:
                    return "Commercial";
                case SHORT:
                    return "Short";
                case BREAK_BUMPER:
                    return "Break Bumper";
                case SCHED_BUMPER:
                    return "Schedule Bumper";
                case STATION_ID:
                    return "Station ID";
                default:
                    return "Unknown";
            }
        }
    }
    public enum EpisodeType {
        NORMAL, SPECIAL, PILOT
    }

    // Path to video, show name, and title
    public String path, show, title;

    // File name without extension
    public transient String name;

    // Start and end of video clip (relative to video start in seconds)
    public double start = 0, end;

    // Stream start time (some MP4 files have non-zero stream starts that aren't accounted for by ffplay)
    public double streamStart = 0;

    // Mid-break times
    public transient double[] midBreaks = new double[0];

    // Start and end of segment (relative to midnight in seconds)
    public double startTime, endTime;

    // Segment type
    public Format format;

    // EPISODE SPECIFIC
    // Episode number, season number, and episode part (1a = 1, 1b = 2, etc.)
    public Integer episode, season;
    public Character part;
    // Episode type
    public EpisodeType epType;

    public Segment() {
    }

    // Derive segment information based on path
    public Segment(String path) {
        File vid = new File(path);
        String dirName = vid.getParentFile().getName();
        /*if(!vid.exists())
            Log.w(TAG, "Video file does not exist " + path);*/
        this.path = path;

        // Get show name (excluding year)
        if (path.startsWith(Config.get("SHOW_DIR"))) {
            // Need to obtain the first directory name under shows
            int s = -1, e = -1;
            for (int i = Config.get("SHOW_DIR").length(); i < path.length(); i++) {
                char c = path.charAt(i);
                if (s == -1 && c != File.separatorChar) {
                    s = i;
                } else if (s != -1 && c == File.separatorChar) {
                    e = i;
                    break;
                }
            }
            if (s > -1 && e > -1)
                show = path.substring(s, e);
        }
        if (show == null || show.isEmpty())
            show = dirName; // Fallback

        // Exclude year the show started
        if (show.contains(" (") && show.endsWith(")"))
            show = show.substring(0, show.indexOf(" ("));

        // Determine segment format
        if (show.equals("Commercials"))
            format = Format.COMMERCIAL;
        else if (dirName.equals("Break Bumpers"))
            format = Format.BREAK_BUMPER;
        else if (show.endsWith("Bumps"))
            format = Format.SCHED_BUMPER;
        else if (show.contains("Shorts"))
            format = Format.SHORT;
        else if (show.equals("Station IDs"))
            format = Format.STATION_ID;
        else
            format = Format.SHOW;

        // Get title and extract available information
        title = vid.getName();
        if(title.contains("."))
            title = title.substring(0, title.lastIndexOf("."));
        name = title;
        if(title.startsWith("(")) {
            // Tag format: (S<season num>E<episode num><optional: part letter>)
            // or (Special) or (Pilot)
            String tag = title.substring(1, title.indexOf(")"));
            switch(tag) {
                case "Special":
                    epType = EpisodeType.SPECIAL;
                    break;
                case "Pilot":
                    epType = EpisodeType.PILOT;
                    break;
                default:
                    try {
                        if(tag.startsWith("S") && tag.contains("E")) {
                            int ei = tag.indexOf("E");
                            season = Integer.parseInt(tag.substring(1, ei));
                            int i = ei + 1;
                            for (; i < tag.length(); i++) {
                                char c = tag.charAt(i);
                                if (c < '0' || c > '9') {
                                    part = c;
                                    break;
                                }
                            }
                            episode = Integer.parseInt(tag.substring(ei + 1, i));
                        } else {
                            // Year instead of episode and season
                            episode = Integer.parseInt(tag);
                        }
                        epType = EpisodeType.NORMAL;
                    } catch(Exception e) {
                        Log.e(TAG, "Illegal video tag: " + tag);
                        epType = null;
                    }
            }
            title = title.substring(title.indexOf(") ") + 2).replace("; ", " / ");
        }
    }

    public Segment(Segment other) {
        path = other.path;
        show = other.show;
        title = other.title;
        name = other.name;
        start = other.start;
        streamStart = other.streamStart;
        midBreaks = new double[other.midBreaks.length];
        System.arraycopy(other.midBreaks, 0, midBreaks, 0, midBreaks.length);
        end = other.end;
        startTime = other.startTime;
        endTime = other.endTime;
        format = other.format;
        episode = other.episode;
        season = other.season;
        part = other.part;
        epType = other.epType;
    }

    public Segment(File vid) {
        this(vid.getAbsolutePath());
    }

    public double getDuration() {
        return end - start;
    }

    public int getDurationMins() {
        return (int) (getDuration() / 60);
    }

    public void setStartTime(double startSecs) {
        startTime = startSecs;
        endTime = startTime + getDuration();
    }

    /**
     * Find index where name differs
     * @param other Other segment
     * @return Index where name differs
     */
    public int compareName(Segment other) {
        int index = 0;
        if(name != null && other.name != null) {
            if(name.equals(other.name))
                return name.length();
            for (int i = 0; i < name.length() && i < other.name.length(); i++) {
                if (name.charAt(i) != other.name.charAt(i))
                    return index;
                // Don't count whitespace
                if (name.charAt(i) == ' ')
                    index = i;
            }
            index = Math.min(name.length(), other.name.length());
        }
        return index;
    }

    @Override
    public String toString() {
        if(format == Format.SHOW) {
            if (episode != null) {
                if (season != null)
                    return String.format("%s\n(S%dE%d%s) %s\n%d minutes", show, season,
                            episode, (part != 0 ? part : ""), title, getDurationMins());
                else
                    return String.format("%s\n(%d) %s\n%d minutes", show, episode, title, getDurationMins());
            } else
                return String.format("%s\n%s\n%d minutes", show, title, getDurationMins());
        }
        double dur = getDuration();
        String durType = "seconds";
        if(dur > 60) {
            dur /= 60;
            durType = "minutes";
        }
        return String.format("%s\n%s\n%1.0f %s", format, title, dur, durType);
    }

    @Override
    public int compareTo(Segment o) {
        if (this.format != Segment.Format.SHOW || o.format != Segment.Format.SHOW)
            return this.name.compareTo(o.name);
        int tComp = this.epType != null && o.epType != null ? this.epType.compareTo(o.epType)
                : (this.epType != null ? 1 : (o.epType != null ? -1 : 0));
        int sComp = this.season != null && o.season != null ? Integer.compare(this.season, o.season)
                : (this.season != null ? 1 : (o.season != null ? -1 : 0));
        int eComp = this.episode != null && o.episode != null ? Integer.compare(this.episode, o.episode)
                : (this.episode != null ? 1 : (o.episode != null ? -1 : 0));
        int pComp = this.part != null && o.part != null ? Character.compare(this.part, o.part)
                : (this.part != null ? 1 : (o.part != null ? -1 : 0));
        if (tComp == 0) {
            if (sComp == 0) {
                if (eComp == 0)
                    return pComp;
                return eComp;
            }
            return sComp;
        }
        return tComp;
    }
}
