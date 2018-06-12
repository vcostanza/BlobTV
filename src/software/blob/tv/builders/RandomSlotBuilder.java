package software.blob.tv.builders;

import software.blob.tv.obj.ChannelInfo;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;

/**
 * Slot builder for shows with random separated episodes
 */
public class RandomSlotBuilder extends SlotBuilder {

    private static final String TAG = "RandomSlotBuilder";

    public RandomSlotBuilder(Playlist segments, ShowInfo info, ChannelInfo channel) {
        super(segments, info, channel);
    }

    @Override
    public Playlist build() {
        Playlist pl;
        if (_segs.hasMetaValue("episodes")) {
            pl = new Playlist(_segs, _segs.getMetaStringArray("episodes", null));
        } else {
            // Remove specials and other crap
            Playlist epList = new Playlist();
            for (Segment s : _segs) {
                if (s.epType == Segment.EpisodeType.NORMAL)
                    epList.add(s);
            }

            pl = new Playlist();
            double remTime = _segs.getSlotSizeSecs() * 0.9;

            // Fill slot with at most 3 episodes
            int epCount = 0;
            while (epCount < 3) {
                Segment s = epList.getRandomSegment(remTime);
                if (s != null) {
                    pl.add(0.0, s);
                    epList.remove(s);
                    remTime -= s.getDuration();
                }
                epCount++;
            }
        }

        finalizePlaylist(pl);
        return pl;
    }
}
