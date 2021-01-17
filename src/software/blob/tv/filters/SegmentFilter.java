package software.blob.tv.filters;

/**
 * A video or audio filter that applies to a specific segment
 *
 * Note: Non-abstract sub-classes MUST have an empty constructor in order for deserialization to work properly
 */
public abstract class SegmentFilter {

    // The type/ID of the filter
    public final String type;

    protected SegmentFilter(String type) {
        this.type = type;
    }

    /**
     * Create a copy of this filter
     * @param copy Copy to write data to (null to create new)
     * @return Filter copy
     */
    public abstract SegmentFilter copy(SegmentFilter copy);

    public SegmentFilter copy() {
        return copy(null);
    }
}
