package software.blob.tv.builders;

import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;

/**
 * Slot builder for single-segment shows
 */
public class DefaultSlotBuilder extends SlotBuilder {

    private static final String TAG = "DefaultSlotBuilder";

    @Override
    public Playlist buildImpl() {
        Playlist pl;

        if (_segs.hasMetaValue("episodes")) {
            pl = new Playlist(_segs, _segs.getMetaStringArray("episodes", null));
        } else {
            pl = new Playlist();
            // Remove specials and pilots
            for (int i = 0; i < _segs.size(); i++) {
                Segment s = _segs.get(i);
                if (s.epType != Segment.EpisodeType.NORMAL)
                    _segs.remove(i--);
            }
            pl.add(0.0, _segs.getRandomSegment(_segs.getSlotSizeSecs() * 0.6, _segs.getSlotSizeSecs()));
        }
        finalizePlaylist(pl);
        return pl;
    }
}
