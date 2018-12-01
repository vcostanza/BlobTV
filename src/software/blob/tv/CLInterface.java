package software.blob.tv;

import software.blob.tv.builders.ScheduleBuilder;
import software.blob.tv.gui.BuilderGUI;
import software.blob.tv.obj.*;
import software.blob.tv.pdf.ScheduleForm;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Command line interface
 */
public class CLInterface {

    private static final String TAG = "CLInterface";

    public static void main(String[] args) {
        Config.load(new File("config.txt"));
        if(args.length == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showGUI();
                }
            });
        } else {
            genRandomSched();
        }
    }

    public static void genRandomSched() {
        ChannelInfo[] cInfos = ChannelInfo.parseChannelList(Config.getFile("CHANNEL_INFO"));
        LogoColors colors = LogoColors.load(Config.getFile("LOGO_COLORS"));
        Map<Integer, Channel> channels = new HashMap<Integer, Channel>();

        for(ChannelInfo c : cInfos) {
            Playlist pl;
            Schedule sched;
            if (c.schedule != null && c.schedule.isDirectory()) {
                // No rules - just fill entire playlist with non-stop episodes
                pl = ScheduleBuilder.buildForShow(c.schedule);

                Log.d(TAG, "Generated channel " + c.number + ": " + c.name);
                double[] gaps = pl.getMaxGaps();
                Log.d(TAG, "Min gap: " + gaps[0] + ", Max gap: " + gaps[1]);

                FileUtils.writeToFile(Config.getFile("CHANNEL_PLAYLISTS_DIR", c.playlist),
                        pl.toJsonString());
                continue;
            }
            Channel copy = channels.get(c.copyChannel);
            if (copy != null) {
                // Copy existing schedule/playlist with time offset
                int offsetMins = c.copyChannelOffset * 60;
                sched = Schedule.copy(copy.schedule, offsetMins);
                pl = new Playlist(copy.playlist);
                pl.timeShift(offsetMins);
                Log.d(TAG, "Generated channel " + c.number + ": " + c.name + " from "
                        + copy.channelInfo.name + " with " + c.copyChannelOffset + " hour offset ");
                // Need to write out schedule for client-side
                if (c.schedule != null) {
                    FileUtils.writeToFile(c.schedule, sched.toJsonString());
                } else {
                    Log.e(TAG, "Missing schedule definition for channel " + c.number);
                }
            } else {
                // Load schedule and generate playlist
                sched = Schedule.load(c.schedule);
                if (sched == null)
                    continue;
                ScheduleBuilder sb = new ScheduleBuilder(c, sched);
                pl = sb.build();
                Log.d(TAG, "Generated channel " + c.number + ": " + c.name);
                double[] gaps = pl.getMaxGaps();
                Log.d(TAG, "Min gap: " + gaps[0] + ", Max gap: " + gaps[1]);
                sched.readEpisodes(pl);
            }
            channels.put(c.number, new Channel(c, sched, pl));

            // Write out JSON file
            FileUtils.writeToFile(Config.getFile("CHANNEL_PLAYLISTS_DIR", c.playlist),
                    pl.toJsonString());

            // Generate schedule PDF
            ScheduleForm form = new ScheduleForm(c, pl, sched, colors);
            try {
                form.generate();
            } catch (IOException e) {
                Log.e(TAG, "Failed to generate form for channel " + c.number, e);
            }
        }
    }

    private static void showGUI() {
        JFrame f = new JFrame("BlobTV Schedule Builder");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(480, 360);
        f.add(new BuilderGUI());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
