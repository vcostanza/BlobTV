package software.blob.tv.filters;

/**
 * A filter with a set position in the video frame
 *
 * Filter positions are defined in a 640x480 space and scaled to match the video size and aspect ratio.
 * This is done by simply taking the position, dividing X by 640 and Y by 480, and then multiplying each by the
 * final video width and height (rounded).
 *
 * For example, a filter that's positioned at x=630 y=450 displayed on a 1920x1080 video will be positioned
 * at x=1890 y=1013 on the final video.
 */
public abstract class PositionFilter extends SegmentFilter {

    // X and Y components defining the filter position in 640x480 space
    public float x, y;

    protected PositionFilter(String type) {
        super(type);
    }

    @Override
    public PositionFilter copy(SegmentFilter copy) {
        if (!(copy instanceof PositionFilter))
            return null;
        PositionFilter ret = (PositionFilter) copy;
        ret.x = this.x;
        ret.y = this.y;
        return ret;
    }
}
