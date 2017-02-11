destfldr=/sdcard/ARO/screenVideos/
dest=$destfldr"video"
#bitrate="--bit-rate 1000000"
#screenSize="--size 1080x1920"
#screenSize="--size 540x960"

seconds=$1
bitrate="--bit-rate $2"
screenSize="--size $3"
CMD=$4

STOPFILE=$destfldr"cmdstop"
rm $STOPFILE

settings=$destfldr"../video.settings"
echo "seconds    = $seconds"         >  $settings
echo "bitrate    = $bitrate"         >> $settings
echo "screenSize = $screenSize"      >> $settings

if [ "$CMD" = "capture" ]; then

#	# create a script to terminate this process
	mypid=$$
	stopvid=$destfldr"stopvid"
	echo "kill -s HUP $mypid">$stopvid

	cat /dev/null > $dest-time;
	delay=$((seconds - 1))
	
	vid=100000
#	vend=$((vid + $segCnt))
	
    while [ ! -f $STOPFILE ];
	do
	    vid=$((vid + 1))
	    suf=${vid:1}
		echo "(echo $EPOCHREALTIME >> $dest-time & screenrecord --time-limit $seconds $bitrate $screenSize $dest$suf.mp4;) &"
		(echo $EPOCHREALTIME >> $dest-time & screenrecord --time-limit $seconds $bitrate $screenSize $dest$suf.mp4;) &
		sleep $delay
	done
	sleep $delay
else
	echo "Error:"
	echo $0
	echo "seconds    = $seconds"
	echo "bitrate    = $bitrate"
	echo "screenSize = $screenSize"
	echo "CMD = $CMD"
fi