#!/bin/bash

# Fun script which takes all the video files in a directory, finds a random clip from each, and puts them together in a single video

outFile="$1"

if [[ $outFile != *.mp4 ]]; then
	echo "You must specify an output video.mp4"
	exit
fi

outName=$(basename "$outFile" ".mp4")

echo "Rendering $outFile"

numEps=0
segs=()
for dir in *; do
	for vid in "$dir/("*".mp4"; do
		segs+=("$vid");
		numEps=$((numEps+1))
	done
done

i=0
iter=200
total=500
streams=()
inputs=()
while [ $i -lt $iter ]; do
	# Get random episode
	rand=$(od -vAn -N4 -tu4 < /dev/urandom)
	randEp=${segs[$((rand % $numEps))]}
	
	timeStamp=$(ffmpeg -i "file:$randEp" 2>&1 | grep 'Duration' | awk '{print $2}' | sed s/,//)
	mins=$(printf "$timeStamp" | awk -F ":" '{print $2}')
	secs=$(printf "$timeStamp" | awk -F ":" '{print $3}')
	dur=$(bc -l <<< "($mins * 60 + $secs - 60) * 1000")
	dur=${dur%.*}
	
	# Get random start time
	rand=$(od -vAn -N4 -tu4 < /dev/urandom)
	randTime=$(bc -l <<< "($((rand % dur)) / 1000) + 60")
	
	echo $randTime
	
	# Get random duration
	#rand=$(od -vAn -N4 -tu4 < /dev/urandom)
	#randDur=$(bc -l <<< "($((rand % 100)) / 1000) + 0.1")
	randDur=0.05
	
	if [[ $randDur != 0.* ]]; then
		randDur="0$randDur"
	fi
	
	outPart="$outName-$i.avi"
	ffmpeg -ss $randTime -i "file:$randEp" -t $randDur -c:a pcm_s16le -c:v rawvideo -s 640x480 -pix_fmt yuv420p -vf "setsar=sar=1/1,setdar=dar=4/3" "output/$outPart"
	if [ -f "output/$outPart" ]; then
		inputs+=("-i output/$outPart")
		streams+=("[$i:0] [$i:1]")	
		i=$((i+1))
	else
		echo "Error reading \"$randEp\"" >> "output/errors.txt"
	fi
done

i=0
randInputs=()
randStreams=()
while [ $i -lt $total ]; do
	rand=$(od -vAn -N4 -tu4 < /dev/urandom)
	randInputs+=(${inputs[$((rand % iter))]})
	randStreams+=(${streams[$((rand % $iter))]})
	i=$((i+1))
done

filter="${randStreams[@]} concat=n=$total:v=1:a=1 [v] [a]"
#echo "ffmpeg ${randInputs[@]} -filter_complex \"$filter\" -map \"[v]\" -map \"[a]\" -y \"output/$outFile\""
ffmpeg ${inputs[@]} -filter_complex "$filter" -map "[v]" -map "[a]" -crf 20 -y "output/$outFile"

i=0
while [ $i -lt $iter ]; do
	rm -f "output/$outName-$i.avi"
	i=$((i+1))
done
