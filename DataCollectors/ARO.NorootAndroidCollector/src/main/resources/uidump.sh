outfile="/sdcard/ARO/UIComparator/"
sleeptime=4
mkdir $outfile
while true
do
	filename=$(date +"%s")".xml"
	uiautomator dump $outfile$filename
	sleep $sleeptime
done
