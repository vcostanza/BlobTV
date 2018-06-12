#!/bin/sh

# Read break timecodes directly from defined chapter timecodes

file="$1"
#tag=$(echo "$file" | awk '{ print $1 }')
name=$(basename "$file" ".mp4")
i=0
chapname=""

echo "\"$name\": {"
for line in $(ffprobe "$file" 2>&1 | grep 'Chapter #' | awk '{print $4}'); do
    if [ $i = 0 ]; then
		chapname="intro"
		line="0.0,"
    elif [ $i = 1 ]; then
        chapname="episode_a"
    elif [ $i = 2 ]; then
        chapname="episode_b"
    else
	    chapname="credits"
	fi
	    
    echo "    \"$chapname\": "$line
    i=$((i+1))
done 
echo "},"
