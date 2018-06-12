package software.blob.tv.obj;

/**
 * A container for the channel info, schedule, and playlist
 */
public class Channel {
    public final ChannelInfo channelInfo;
    public final Schedule schedule;
    public final Playlist playlist;

    public Channel(ChannelInfo info, Schedule sched, Playlist pl) {
        this.channelInfo = info;
        this.schedule = sched;
        this.playlist = pl;
    }
}
