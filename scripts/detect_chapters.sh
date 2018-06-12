#!/bin/bash

# Detects appropriate break times for an episode based on black frame criteria
# Results are dumped to logs/all.log
# Dumping to info.js directly is not recommended since there will probably be multiple black scenes per scan internval

### Options __________________________________________________________________________________________________________
dur=0.1                           # Set the minimum detected black duration (in seconds)
amount=0.999                       # Set the threshold for considering a picture as "black" (in percent)
thresh=0.1                        # Set the threshold for considering a pixel "black" (in luminance)

filter="blackdetect=d=$dur:pix_th=$thresh:pic_th=$amount"

video="$1"
arg2="$2"

if [ "$#" -gt 1 ] && [ $arg2 = "full" ]; then
	ffmpeg -i "$video" -vf "$filter" -f NULL /dev/null
	exit 0
fi

# Scan start times (scanning the entire video takes longer and might not give us a legit break)
START=1
END=5
seg1="00:00:30"
seg2="00:04:00"
seg3="00:09:00"
seg4="00:16:00"
seg5="00:21:00"

# Time to continue scan after start time
scan1="00:01:00"
scan2="00:03:00"
scan3="00:03:00"
scan4="00:03:00"
scan5="00:02:00"
segnames=("intro" "episode_a" "episode_b" "episode_c" "episode_d" "credits")

### Main Program ______________________________________________________________________________________________________

name=$(basename "$video" ".mp4")

### Set path to logfile
logfile="logs/$name".log
logall="logs/all.log"
mkdir -p "logs"

### analyse each video with ffmpeg and search for black scenes
echo "Analyzing $name for black frames..."
#printf "" > "$logfile"
printf "\"$name\": {\n" >> "$logall"
if [ $START -gt 0 ]; then
    printf "    \"${segnames[0]}\": 0.0,\n" >> "$logall"
fi
for (( c=$START; c<=$END; c++ )); do
	# Intro/Episode A scan
	echo "Scanning for segment $c"
	seg=seg${c}
	scan=scan${c}
	printf "" > "$logfile"."$c"
	ffmpeg -ss ${!seg} -i "$video" -t ${!scan} -vf "$filter" -f NULL -y /dev/null 2>&1 | grep "black_start:" | grep -o "black_start:.*" > "$logfile"."$c"
	secs=$(echo ${!seg} | awk -F: '{ print ($1 * 3600) + ($2 * 60) + $3 }')
	#printf "\n --- SCAN $c [ ${!seg} ($secs) ] ---\n" >> "$logfile"
	fmt="%8.6f"
	if [ $c -lt $END ]; then
		fmt="$fmt",
	fi
	fmt="$fmt\n"
	b_start="null"
	b_end="null"
	b_dur="null"	
	if [ $c = 0 ] && [ ! -s "$logfile"."$c" ]; then
		printf "    \"${segnames[$c]}\": 0.0,\n" >> "$logall"
	fi
	for line in $(cat "$logfile"."$c"); do
		if grep -q "black_start:" <<< "$line"; then
			b_start=${line#black_start:}
		elif grep -q "black_end:" <<< "$line"; then
			b_end=${line#black_end:}
		elif grep -q "black_duration:" <<< "$line"; then
			b_dur=${line#black_duration:}
		fi
		if [ $b_start != "null" ] && [ $b_end != "null" ] && [ $b_dur != "null" ]; then
			split="$b_dur * 0.5"
			if [ $c = 0 ]; then
				split="$b_dur * 0.9"
			fi
			printf "    \"${segnames[$c]}\": $fmt" $(bc -l <<< "$b_start + $split + $secs") >> "$logall"
			#printf $fmt $(bc -l <<< "$b_start + $b_dur / 2.0 + $secs") >> "$logfile"
			b_start="null"
			b_end="null"
			b_dur="null"
		fi
	done
	rm "$logfile"."$c"
done
printf "},\n" >> $logall
