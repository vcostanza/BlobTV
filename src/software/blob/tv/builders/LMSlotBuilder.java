package software.blob.tv.builders;

import software.blob.tv.obj.ChannelInfo;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;
import software.blob.tv.util.Log;

/**
 * Slot builder for Looney Melodies
 */
public class LMSlotBuilder extends SlotBuilder {

    private static final String TAG = "LMSlotBuilder";

    public LMSlotBuilder(Playlist segments, ShowInfo info, ChannelInfo channel) {
        super(segments, info, channel);
    }

    @Override
    public Playlist build() {
        Playlist pl;
        if (_segs.hasMetaValue("episodes")) {
            pl = new Playlist(_segs, _segs.getMetaStringArray("episodes", null));
        } else {
            pl = new Playlist();
            // Three shorts from 30s, 40s, and 50/60s respectively
            Playlist thirties = new Playlist();
            Playlist fourties = new Playlist();
            Playlist fifsixties = new Playlist();

            for (Segment s : _segs) {
                if (s.episode >= 1930 && s.episode < 1940)
                    thirties.add(s);
                else if (s.episode >= 1940 && s.episode < 1950)
                    fourties.add(s);
                else if (s.episode >= 1950 && s.episode < 1970)
                    fifsixties.add(s);
            }

            double remTime = _segs.getSlotSizeSecs();
            try {
                Segment seg1 = thirties.getRandomSegment(remTime);
                remTime -= seg1.getDuration();
                Segment seg2 = fourties.getRandomSegment(remTime);
                remTime -= seg2.getDuration();
                Segment seg3 = fifsixties.getRandomSegment(remTime);
                pl.add(0.0, seg1);
                pl.add(0.0, seg2);
                pl.add(0.0, seg3);
            } catch (Exception e) {
                Log.w(TAG, "Trouble adding episodes: ", e);
            }
        }

        finalizePlaylist(pl);
        return pl;
    }
}
