package software.blob.tv.builders;

import software.blob.tv.filters.SegmentFilter;
import software.blob.tv.filters.TextFilter;
import software.blob.tv.obj.*;
import software.blob.tv.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Build a schedule slot - meant to be extended upon
public abstract class SlotBuilder {

    private static final String TAG = "SlotBuilder";

    protected Playlist _segs;
    protected ShowInfo _info;
    protected ChannelInfo _channel;
    protected Schedule _schedule;
    protected ScheduleSlot _schedSlot;

    /**
     * Set the elligible show segments (required)
     * @param segs Segments playlist
     */
    public SlotBuilder setSegments(Playlist segs) {
        _segs = segs;
        return this;
    }

    /**
     * Set the show metadata (required)
     * @param info Show info
     */
    public SlotBuilder setShowInfo(ShowInfo info) {
        _info = info;
        return this;
    }

    /**
     * Set the channel metadata (required)
     * @param channel Channel info
     */
    public SlotBuilder setChannelInfo(ChannelInfo channel) {
        _channel = channel;
        return this;
    }

    /**
     * Set the schedule data (required if using schedule bumpers)
     * @param sched Schedule
     */
    public SlotBuilder setScheduleInfo(Schedule sched, ScheduleSlot slot) {
        _schedule = sched;
        _schedSlot = slot;
        return this;
    }

    /**
     * Build playlist for this slot in the schedule
     * Calls {@link #buildImpl()} after some preliminary checks
     * @return Playlist
     */
    public Playlist build() {
        if (_segs == null) {
            Log.e(TAG, "Missing segments!");
            return null;
        }
        if (_info == null) {
            Log.e(TAG, "Missing show info!");
            return null;
        }
        if (_channel == null) {
            Log.e(TAG, "Missing channel info!");
            return null;
        }
        return buildImpl();
    }

    protected abstract Playlist buildImpl();

    // Handle intro and credits
    protected void finalizePlaylist(Playlist pl) {
        pl.setSlotSize(_segs.getSlotSize());
        boolean skipIntro = false, skipBumper = false, skipCredits = false;
        if(_info.breaks != null) {
            // Check for intro skip
            for (Segment s : pl) {
                ShowInfo.Break br = _info.breaks.get(s.name);
                if (br != null) {
                    if (br.hasIntro)
                        skipIntro = true;
                    if (br.hasBumper)
                        skipBumper = true;
                    if (br.hasCredits)
                        skipCredits = true;
                }
            }
        }

        // Add intro and credits if specified
        Segment intro = getIntroSeg();
        Segment credits = getCreditsSeg();
        if(intro != null && !skipIntro)
            pl.add(0.0, intro, true);
        if(credits != null && !skipCredits)
            pl.add(0.0, credits);

        // Insert schedule bumper w/ appropriate upcoming and later show
        if (_channel.bumps != null)
            insertScheduleBumper(pl);

        // Insert station id (if we can)
        Segment stationId = _channel.stationIds.getRandomSegment(pl.getDeadAir());
        if(stationId != null)
            pl.add(0.0, stationId);

        // Insert short (if applicable and we can)
        if(stationId != null && _channel.shorts != null) {
            Segment shortFilm = _channel.shorts.getRandomSegment(pl.getDeadAir());
            if (shortFilm != null)
                pl.add(pl.last().startTime, shortFilm, true);
        }

        // Insert mid-break bumper (if they exist)
        if (!skipBumper)
            insertBreakBumpers(pl);

        // Commercials (lowest priority)
        insertCommercials(pl);

        // Filter post-processing (such as text line wrapping)
        // XXX - Ideally this would be performed during serialization (via some "toJson" method override)
        // but we can't really do this with Gson's automatic serialization
        postProcessFilters(pl);
    }

    /**
     * Insert a schedule bumper before the next show
     * @param pl Playlist
     */
    protected void insertScheduleBumper(Playlist pl) {
        // Can't build schedule bumpers without a schedule
        if (_schedule == null) {
            Log.e(TAG, "Failed to insert schedule bumper - schedule hasn't been set");
            return;
        }

        // Pull random schedule bumper
        Segment bump = _channel.bumps.getRandomSegment(pl.getDeadAir());
        if (bump == null) {
            Log.e(TAG, "Not enough dead air to fit schedule bumper after " + pl.last());
            return;
        }

        // Need to modify text filters for the upcoming shows
        // Schedule bumpers should already have base filters defined via "filters.js"
        if (!bump.hasFilters()) {
            Log.e(TAG, "Schedule bumper is missing filters: " + bump);
            return;
        }

        // Use a copy so the base filters aren't modified
        bump = new Segment(bump);

        // Parse text filter data
        int index = _schedule.indexOf(_schedSlot);
        for (SegmentFilter filter : bump.filters) {
            // Only care about text filters
            if (!(filter instanceof TextFilter))
                continue;

            TextFilter tf = (TextFilter) filter;

            // Insert show name
            int showNum = tf.getVariableNum("show");
            if (showNum != -1) {
                ScheduleSlot slot = _schedule.get(index + showNum, true);
                tf.setVariable("show" + showNum, slot.Show);
            }

            // Insert show time
            int timeNum = tf.getVariableNum("time");
            if (timeNum != -1) {
                ScheduleSlot slot = _schedule.get(index + timeNum, true);
                tf.setVariable("time" + timeNum, slot.getFormattedTime(false));
            }
        }

        // Add the bumper
        pl.add(0.0, bump);
    }

    /**
     * Insert a bumper between the middle commercial break
     * @param pl Playlist
     */
    protected void insertBreakBumpers(Playlist pl) {
        File breaksDir = new File(_info.showDir, "Break Bumpers");
        if (!breaksDir.exists())
            return;

        Playlist breakBumpers = new Playlist(breaksDir);
        Playlist breaks = pl.getBreakSegs();
        for (Segment s : breaks) {
            if (s.format == Segment.Format.SHOW) {
                Segment br = breakBumpers.getRandomSegment(pl.getDeadAir());
                if (br != null) {
                    pl.add(s.endTime, br, true);
                    breakBumpers.remove(br);
                } else
                    break;
            }
        }
    }

    /**
     * Insert commercials to fill in any remaining dead air
     * @param pl Playlist
     */
    protected void insertCommercials(Playlist pl) {
        if(pl.isEmpty())
            return;

        // Calculate number of breaks
        int numBreaks = pl.getNumBreaks();
        if (numBreaks == 0)
            return;

        // Get a bunch of random commercials
        Playlist commList = new Playlist(_channel.commercials);
        List<Playlist> comms = new ArrayList<>(numBreaks);
        double deadAir = pl.getDeadAir();
        double remTime = deadAir;
        int i = 0;
        while(remTime > 0) {

            // Get and insert random commercial
            Segment commercial = commList.getRandomSegment(remTime);
            if(commercial == null)
                break;

            // Remove duplicates
            for(int j = 0; j < commList.size(); j++) {
                Segment c = commList.get(j);
                if(commercial.compareName(c) >= 3)
                    commList.remove(j--);
            }

            // Update remaining time
            remTime -= commercial.getDuration();

            // Determine whether or not to move to the next bucket
            double target = (deadAir / numBreaks) * (i + 1);
            double endTime = deadAir - remTime;
            if(endTime >= target && endTime - target > commercial.getDuration() / 2)
                i++;

            // Store commercial in bucket
            if (i >= comms.size())
                comms.add(new Playlist());
            comms.get(i).add(commercial);
        }

        // Stop if there's no room for commercials
        if (comms.isEmpty()) {
            // Remove any break bumpers
            List<Segment> breakBumps = pl.findAllByFormat(Segment.Format.BREAK_BUMPER);
            for (Segment s : breakBumps)
                pl.remove(s);
            pl.stretchDuration(pl.getSlotSizeSecs());
            return;
        }

        // Determine which segments to insert commercials after
        Playlist slots = pl.getBreakSegs();

        // Insert each commercial bucket
        i = 0;
        for (Segment s : slots) {
            double startTime = s.endTime;
            for (Segment c : comms.get(i)) {
                pl.add(startTime, c, true);
                startTime += c.getDuration();
            }
            if (++i >= comms.size())
                break;
        }

        pl.stretchDuration(pl.getSlotSizeSecs());
    }

    /**
     * Filter post-processing
     * @param pl Playlist
     */
    protected void postProcessFilters(Playlist pl) {
        for (Segment seg : pl) {
            if (!seg.hasFilters())
                continue;

            for (int i = 0; i < seg.filters.size(); i++) {

                SegmentFilter filter = seg.filters.get(i);

                if (filter instanceof TextFilter) {
                    // Wrap text
                    List<TextFilter> wrapped = ((TextFilter) filter).wrapLines();

                    // Replace with wrapped lines of text
                    seg.filters.remove(i);
                    seg.filters.addAll(i, wrapped);
                    i += wrapped.size() - 1;
                }
            }
        }
    }

    // Intermediate build step for adding intro
    protected Segment getIntroSeg() {
        if(_info.introVid != null) {
            Segment intro = _segs.findByPath(_info.introVid.getPath());
            if(intro == null)
                Log.w(TAG, "Failed to find intro segment: " + _info.introVid);
            else
                intro.title = "Intro";
            return intro;
        }
        return null;
    }

    // Intermediate build step for adding credits
    protected Segment getCreditsSeg() {
        if(_info.creditsVid != null) {
            Segment credits = _segs.findByPath(_info.creditsVid.getPath());
            if(credits == null)
                Log.w(TAG, "Failed to find credits segment: " + _info.creditsVid);
            else
                credits.title = "Credits";
            return credits;
        }
        return null;
    }

    /**
     * Return specific SlotBuilder instance based on show info
     * This is how to properly instantiate a SlotBuilder
     * @param info Show information
     * @return Matching slot builder
     */
    public static SlotBuilder create(ShowInfo info) {
        if(info.separatedEps && info.episodes != null)
            // Episodes are made up of separate video files
            return new SESlotBuilder();
        else if(info.randomized)
            // Random episodes are picked
            return new RandomSlotBuilder();
        // Everything else
        return new DefaultSlotBuilder();
    }
}
