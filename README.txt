BlobTV Schedule Builder

This utility generates a JSON playlist of video files to be played at specific times during the day.
Also included are some tools to aid with building these playlist files.
Also keep in mind that this was designed for my own personal use, so it's probably not very intuitive.

== Dependencies ==

gson-2.6.2 (serialization to and from JSON)
jfreechart-1.0.19 (for the commercial graph)
pdfbox-app-2.0.2 (for the schedule form generator)


== Requirements ==

 - Properly configured BTV Home directory (default: "/home/vc/BlobTV").
 - A video player capable of interpreting the BTV playlist format (I have a modified version of ffplay which does this).
 
 
== Terminology ==

Segment - A scheduled video segment. Fields are defined as follows:

  path - The path to the video file. Relative to $BTV_HOME.
  show - The name of the show or directory.
  title - The title of the episode or segment
  start - The relative position to start playing the video (seconds; usually 0.0)
  end - The relative end position to stop playing the video (seconds; usually video duration)
  streamStart - The start of the video stream (seconds, almost always 0.0)
  startTime - The scheduled start time of this segment relative to midnight (seconds)
  endTime - The sechedule end time of this segment relative to midnight (seconds)
  format - The segment type (SHOW, COMMERCIAL, SHORT, BREAK_BUMPER, SCHED_BUMPER, STATION_ID)
  episode - The episode number (only applies to SHOW segments)
  season - The season number (only applies to SHOW segments)
  part - The part letter ('A', 'B', etc.; only applies to SHOW segments)
  epType - The type of episode (INVALID, NORMAL, SPECIAL, PILOT; only applies to SHOW segments)
  
Playlist - An array containing video segments, in the order that they are played, over a 24-hour period.
           Gaps in end and start times are considered "dead air" (blank screen).
  
Break - A time slot used to insert commercials or other content.
        Appropriate break times for each video segment are defined in the show's "info.js" file ($BTV_HOME/shows/<show name>/info.js)
        Format:
        Key = File name w/out file extension
        Value = {"intro", "episode_a", "episode_b", "episode_...", "credits", "end"}
        
        Each field within the value defined the break in seconds relative to the video start time
        "intro" is the timecode to start the episode (default: 0.0, skips any dead air in the beginning of the file)
        "end" is the timecode to end the episode (default: duration, skips any dead air at the end of the file)
        "episode_a/b/c/etc." is each timecode where a commercial break can be appropriately inserted
        "credits" is the timecode where the credits for the episode begin (currently unused)
        
Show Info - Metadata associated with a show. Fields are defined as follows:
  
  Runtime - The run time of the show (minutes; usually 30)
  SeparatedEps - Set to "true" if the episodes are already separated by breaks (default: false)
  Randomized - Set to "true" to schedule random episodes (default: false, only used if SeparatedEps="true")
  Episodes - A 2D array containing the complete episode list (only used if SeparatedEps="true")
  Breaks - The break times for each episode (only used if SeparatedEps="false")
  
Duration - The defined duration of each video file in a given show directory ($BTV_HOME/shows/<show name>/durations.js)
           These are calculated ahead of time to significantly improve the speed of the schedule generation process.
           Format:
           Key = File name w/out file extension
           Value = Duration of the video file

Schedule Slot - A defined time slot for a specific show to play. Fields are defined as follows:

  TimeSlot - The time to play the show relative to midnight (minutes)
  Show - The name of the show (must match the show directory name)
  
  optional:
  Episode - The name of the episode to play (must match the video file name without extension)
  Episodes - Array of episodes to play (same as above)
  
Schedule - An array of schedule slots covering a 24-hour period.

Channel - Metadata associated with a playlist. Fields are defined as follows:

  Name - The name of the channel (currently unused)
  Number - The channel number
  Logo - The watermark image displayed in the bottom-right over show segments (relative to $BTV_HOME/i/)
  Schedule - The channel schedule JSON file (relative to $BTV_HOME/js_shared/channels/)
  Playlist - The playlist JSON file (relative to $BTV_HOME/js/channels/)
  
  optional:
  Commercials - The commercials directory (default: "Commercials", relative to $BTV_HOME/shows/)
  Bumpers - The schedule bumpers directory (default: "Schedule Bumps", relative to $BTV_HOME/shows/)
  IDs - The station IDs directory (default: "Station IDs", relative to $BTV_HOME/shows/)
  Shorts - The shorts directory (default: "Shorts", relative to $BTV_HOME/shows/)
           If this is defined then a short cartoon will be played at the end of each time slot (instead of commercials)
  CopyChannel - The channel number to copy the playlist from (rewatch schedule from earlier/later)
  CopyChannelOffset - The time offset of this channel (i.e. 3 = three hours before copied channel, -3 = three hours ahead)
 
Schedule Form - A PDF generated by this application which shows the schedule for a given day ($BTV_HOME/downloads/<channel_#>/Schedule_CH#_yyyy-MM-dd)
 
== Usage ==

java -jar BlobTV.jar <autogen=1>
No arguments = launch GUI
>=1 arguments = automatically generate playlist (for use with a cron job usually)


== GUI ==

Generate Schedule - Builds a playlist for each channel defined in the "channels.js" file ($BTV_HOME/js_shared/channels.js)
Check Breaks - Tool used to verify that commercial break markers are in the correct spot
Commercial Graph - Creates a pie graph of all commercial types. Used to check that there aren't too many Bob's Discount Furniture commercials.


== Example Directory Structure ==

$BTV_HOME
  -downloads
    -channel_1
      -Schedule_CH1_2018-06-11.pdf
    -channel_2
      -Schedule_CH2_2018-06-11.pdf
  -i
    -logo1.svg
    -logo2.svg
  -js
    -schedule_chan_1.js
    -schedule_chan_2.js
  -js_shared
    -channels
      -channel_1.js
      -channel_2.js
      -channels.js
  -shows
    -Dilbert
    -Rocky & Bullwinkle
    -Space Ghost Coast to Coast
    -Commercials
    -Shorts


== FAQ ==

Q: Why?
A: To simulate the experience of watching live TV without paying for cable or some TV-like service.

Q: Why would you want commercials?
A: They make it very easy to align the schedule to 15-minute or 30-minute time slots without having tons of dead air. Also muh realism.

Q: Can I get rid of the commercials?
A: Yes (see "Shorts" field in Channel definition). But keep in mind that due to the alignment issue mentioned above,
   you may experience several seconds or even minutes of dead air if your content doesn't fit together well.
   Also there is currently no gapless "unaligned" mode because I haven't felt the need to add something like that.
