#!/bin/sh
killfile=$1"/killgeteventscript.sh"
echo "kill -s HUP" $$ "; rm $killfile"> $killfile
outfile=$1"/geteventslog"

rm $outfile
echo $outfile
touch $outfile

val=`cat /proc/timer_list | grep now`;
echo "$val" >> $outfile

getevent -lt >> $outfile