package software.blob.tv.filters;

/**
 * A simple filter for fading in and out of a video
 *
 * This filter currently only takes 2 parameters:
 * fadeInEnd - The video timestamp at which the fade in ends (null to ignore)
 * fadeOutStart - The video timestamp at which the fade out begins (null to ignore)
 *
 * Note: fadeOutStart may be set to a negative value. This means the fade out will begin that many seconds
 * before the video ends. I.e. fadeOutStart = -1 will start fading the video out 1 second before it ends.
 *
 * TODO: More advanced options - Not really required at the moment, but could be useful
 */
public class FadeFilter extends SegmentFilter {

    // The timestamp where the video should finish fading in (seconds)
    public float fadeInEnd;

    // The timestamp where the video should begin fading out (seconds)
    // May be set to a negative value (fade is offset relative to the video duration)
    public float fadeOutStart;

    // Fade audio in and out along with the video
    public boolean fadeAudio;

    public FadeFilter() {
        super("fade");
    }

    @Override
    public FadeFilter copy(SegmentFilter copy) {
        FadeFilter ret = copy instanceof FadeFilter ? (FadeFilter) copy : new FadeFilter();
        ret.fadeInEnd = this.fadeInEnd;
        ret.fadeOutStart = this.fadeOutStart;
        ret.fadeAudio = this.fadeAudio;
        return ret;
    }
}
