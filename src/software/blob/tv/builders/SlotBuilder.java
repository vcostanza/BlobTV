package software.blob.tv.builders;

import software.blob.tv.obj.ChannelInfo;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;
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

    protected SlotBuilder(Playlist segs, ShowInfo info, ChannelInfo channel) {
        _segs = segs;
        _info = info;
        _channel = channel;
    }

    protected abstract Playlist build();

    // Handle intro and credits
    protected void finalizePlaylist(Playlist pl) {
        pl.setSlotSize(_segs.getSlotSize());
        boolean skipIntro = false, skipCredits = false;
        if(_info.breaks != null) {
            // Check for intro skip
            for (Segment s : pl) {
                ShowInfo.Break br = _info.breaks.get(s.name);
                if (br != null) {
                    if (br.hasIntro)
                        skipIntro = true;
                    else if (br.hasCredits)
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

        // Insert schedule bumper
        if (_channel.bumps != null)
            pl.add(0.0, _channel.bumps.getRandomSegment(pl.getDeadAir()));

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
        File breaksDir = new File(_info.showDir, "Break Bumpers");
        if (breaksDir.exists()) {
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

        // Finally commercials (lowest priority)
        insertCommercials(pl);
    }

    // Insert commercials to fill in remaining dead air
    protected void insertCommercials(Playlist pl) {
        if(pl.isEmpty())
            return;

        // Calculate number of breaks
        int numBreaks = pl.getNumBreaks();
        if (numBreaks == 0)
            return;

        // Get a bunch of random commercials
        Playlist commList = new Playlist(_channel.commercials);
        List<Playlist> comms = new ArrayList<Playlist>(numBreaks);
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
     * @param segs List of videos used during build()
     * @param info Show information
     * @param channel Channel information
     * @return Matching slot builder
     */
    public static SlotBuilder create(Playlist segs, ShowInfo info, ChannelInfo channel) {
        if(info.showDir.contains("Looney Melodies"))
            return new LMSlotBuilder(segs, info, channel);
        else if(info.showDir.contains("Woody Woodpecker"))
            return new WWSlotBuilder(segs, info, channel);
        else if(info.separatedEps && info.episodes != null)
            // SpongeBob, 2 Stupid Dogs, Space Ghost
            return new SESlotBuilder(segs, info, channel);
        else if(info.randomized)
            // What a Cartoon!, O Canada
            return new RandomSlotBuilder(segs, info, channel);
        // Everything else
        return new DefaultSlotBuilder(segs, info, channel);
    }
}
