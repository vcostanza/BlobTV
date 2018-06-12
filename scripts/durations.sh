#!/bin/bash

# Read all video files in a directory and dump each of their durations to durations.js

filter="$1"

for dir in "$filter"*/; do
	echo "Scanning starts and durations for "$(basename "$dir" "/")
	output_s="$dir""starts.js"
	output_d="$dir""durations.js"
	firstFile=0

	printf "{\n" > "$output_d"
	printf "{\n" > "$output_s"
	for video in "$dir"*.mp4; do
		if [ $firstFile = 0 ]; then
			firstFile=1
		else
			printf ",\n" >> "$output_d"
			printf ",\n" >> "$output_s"
		fi
		timeStamp=$(ffmpeg -i "file:$video" 2>&1 | grep 'Duration')
		tsStart=$(printf "$timeStamp" | awk '{print $4}' | sed s/,//)
		tsDur=$(printf "$timeStamp" | awk '{print $2}' | sed s/,//)
		name=$(basename "$video" ".mp4" | sed -e s/%/%%/g)
		hours=$(printf "$tsDur" | awk -F ":" '{print $1}')
		mins=$(printf "$tsDur" | awk -F ":" '{print $2}')
		secs=$(printf "$tsDur" | awk -F ":" '{print $3}')
		dur=$(bc -l <<< "$hours * 3600 + $mins * 60 + $secs")
		printf "\t\"$name\": $dur" >> "$output_d"
		printf "\t\"$name\": $tsStart" >> "$output_s"
	done
	printf "\n}\n" >> "$output_d"
	printf "\n}\n" >> "$output_s"
done
