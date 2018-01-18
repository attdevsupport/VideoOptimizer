outfile="/sdcard/ARO/UIComparator/"
echo "kill -s HUP" $$ > $1"killuidump.sh"
sleeptime=4
mkdir $outfile
while true
do
	filename=$(date +"%s")".xml"
	uiautomator dump $outfile$filename
	sleep $sleeptime
done
