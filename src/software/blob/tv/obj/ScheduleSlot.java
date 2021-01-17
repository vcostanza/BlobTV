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
        this.Episodes = new ArrayList<>(other.Episodes);
    }

    /**
     * Get the time this slot is played as a human-readable string
     * @param incAMPM True to include AM/PM
     * @return Time string
     */
    public String getFormattedTime(boolean incAMPM) {
        int hour = TimeSlot / 60;
        int minutes = TimeSlot % 60;
        String ampm = "";
        if (incAMPM) {
            ampm = "AM";
            if (hour >= 12)
                ampm = "PM";
        }
        hour = hour % 12;
        return String.format(Locale.US, "%d:%02d %s", hour == 0 ? 12 : hour, minutes, ampm).trim();
    }

    public String getFormattedTime() {
        return getFormattedTime(true);
    }

    public void addEpisode(String name) {
        if (Episodes == null)
            Episodes = new ArrayList<>();
        if (!Episodes.contains(name))
            Episodes.add(name);
    }

    public String getEpisodeString() {
        StringBuilder sb = new StringBuilder();
        for (String name : Episodes) {
            if (!name.equals("Intro") && !name.equals("Credits")
                    && !name.endsWith("_credits")) {
                String title = name.substring(name.indexOf(") ") + 2).replace("; ", " / ");
                if (sb.length() > 0)
                    sb.append(" / ");
                sb.append(title);
            }
        }
        return sb.toString();
    }
}
