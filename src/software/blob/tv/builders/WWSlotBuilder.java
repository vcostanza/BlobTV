package software.blob.tv.builders;

import software.blob.tv.obj.ChannelInfo;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;
import software.blob.tv.util.RandUtils;

import java.util.Collections;
import java.util.Comparator;

/**
 * Slot builder for Woody Woodpecker
 */
public class WWSlotBuilder extends SlotBuilder {

    private static final String TAG = "WWSlotBuilder";

    private static final Comparator<Segment> YEAR_COMPARATOR = new Comparator<Segment>() {
        public int compare(Segment s1, Segment s2) {
            return s1.episode - s2.episode;
        }
    };

    public WWSlotBuilder(Playlist segments, ShowInfo info, ChannelInfo channel) {
        super(segments, info, channel);
    }

    @Override
    public Playlist build() {
        Playlist pl;
        if (_segs.hasMetaValue("episodes")) {
            pl = new Playlist(_segs, _segs.getMetaStringArray("episodes", null));
        } else {
            pl = new Playlist();
            Collections.sort(_segs, YEAR_COMPARATOR);
            int randIndex = RandUtils.rand(_segs.size());
            for (int i = 0; i < 3; i++)
                pl.add(0.0, _segs.get(randIndex + i));
        }
        finalizePlaylist(pl);
        return pl;
    }
}
