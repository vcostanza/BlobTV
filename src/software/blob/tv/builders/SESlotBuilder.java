package software.blob.tv.builders;

import software.blob.tv.obj.ChannelInfo;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;
import software.blob.tv.util.Log;
import software.blob.tv.util.RandUtils;

import java.util.Arrays;

/**
 * Slot builder for shows with defined episode sets
 */
public class SESlotBuilder extends SlotBuilder {

    private static final String TAG = "SESlotBuilder";

    public SESlotBuilder(Playlist segments, ShowInfo info, ChannelInfo channel) {
        super(segments, info, channel);
    }

    @Override
    public Playlist build() {
        Playlist pl = new Playlist();

        String[] epSet;
        Segment[] segSet;
        boolean invalidSet;
        int attempts = 0;

        // Verify episode sets - debug only
        //verifySets();

        // Find valid episode set
        do {
            // Get random episode set
            epSet = (String[]) RandUtils.randomItem(_info.episodes);

            if (attempts == 0 && _segs.hasMetaValue("episodes")) {
                Playlist eps = new Playlist(_segs, _segs
                        .getMetaStringArray("episodes", null));
                segSet = eps.toArray(new Segment[eps.size()]);
            } else
                segSet = findSegments(epSet);
            invalidSet = false;
            double totalTime = 0.0;
            for (Segment s : segSet) {
                if (s == null) {
                    invalidSet = true;
                    Log.w(TAG, "Episode set " + Arrays.toString(epSet)
                            + " contains an invalid episode.");
                } else
                    totalTime += s.getDuration();
            }
            if(totalTime > _segs.getSlotSizeSecs()) {
                invalidSet = true;
                Log.w(TAG, "Episode set " + Arrays.toString(epSet)
                        + " total duration is longer than " + _segs.getSlotSize() + " minutes. ");
            }
            if(++attempts == 10) {
                segSet = new Segment[0];
                Log.e(TAG, "Failed to find a valid episode set after 10 attempts...");
            }
        } while(attempts < 10 && invalidSet);

        // Add each segment to playlist
        for(Segment s : segSet)
            pl.add(0, s);

        finalizePlaylist(pl);
        return pl;
    }

    /**
     * Find segments that match episode set
     * @param epSet Episode set (string array of names)
     * @return Corresponding segment set
     */
    private Segment[] findSegments(String[] epSet) {
        Segment[] segSet = new Segment[epSet.length];
        // Convert to corresponding segments
        for(Segment s : _segs) {
            for(int i = 0; i < epSet.length; i++) {
                if(s.name.equals(epSet[i])) {
                    segSet[i] = s;
                    break;
                }
            }
        }
        return segSet;
    }

    /**
     * Debugging tool
     * Verify all episode sets contain valid episode names
     */
    private void verifySets() {
        for (String[] e : _info.episodes) {
            for (Segment s : findSegments(e)) {
                if (s == null)
                    Log.w(TAG, "Episode set " + Arrays.toString(e)
                            + " contains an invalid episode.");
            }
        }
    }
}
