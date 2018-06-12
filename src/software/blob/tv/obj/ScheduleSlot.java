package software.blob.tv.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Time slot with show and (optional) episode
 */
public class ScheduleSlot {
    public int TimeSlot;
    public String Show;
    public String Episode;
    public List<String> Episodes;

    public ScheduleSlot(ScheduleSlot other) {
        this.TimeSlot = other.TimeSlot;
        this.Show = other.Show;
        this.Episode = other.Episode;
        this.Episodes = new ArrayList<String>(other.Episodes);
    }

    public String getFormattedTime() {
        int hour = TimeSlot / 60;
        int minutes = TimeSlot % 60;
        String ampm = "AM";
        if (hour >= 12)
            ampm = "PM";
        hour = hour % 12;
        return String.format(Locale.US, "%d:%02d %s", hour == 0 ? 12 : hour, minutes, ampm);
    }

    public void addEpisode(String name) {
        if (Episodes == null)
            Episodes = new ArrayList<String>();
        if (!Episodes.contains(name))
            Episodes.add(name);
    }

    public String getEpisodeString() {
        String ret = "";
        for (String name : Episodes) {
            if (!name.equals("Intro") && !name.equals("Credits")
                    && !name.endsWith("_credits")) {
                String title = name.substring(name.indexOf(") ") + 2).replace("; ", " / ");
                if (!ret.isEmpty())
                    ret += " / ";
                ret += title;
            }
        }
        return ret;
    }
}
