#!/bin/sh
outfile=$1"/"$2
killfile=$1"/"$3
echo "kill -s HUP" $$ "; rm $killfile" > $killfile

# sleep time in seconds
sleepSeconds=5

aPaths[0]=/sys/class/thermal/thermal_zone0
aPaths[1]=/sys/class/thermal/thermal_zone1
aPaths[2]=/sys/class/thermal/thermal_zone2
aPaths[3]=/sys/class/thermal/thermal_zone3
aPaths[4]=/sys/class/thermal/thermal_zone4
aPaths[5]=/sys/class/thermal/thermal_zone5
aPaths[6]=/sys/class/thermal/thermal_zone6
aPaths[7]=/sys/class/thermal/thermal_zone7
aPaths[8]=/sys/class/thermal/thermal_zone8
aPaths[9]=/sys/class/thermal/thermal_zone9
aPaths[10]=/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp;
aPaths[11]=/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp;
aPaths[12]=/sys/devices/platform/s5p-tmu/curr_temp;
aPaths[13]=/sys/class/i2c-adapter/i2c-4/4-004c/temperature;
aPaths[14]=/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature;
aPaths[15]=/sys/devices/platform/omap/omap_temp_sensor.0/temperature;
aPaths[16]=/sys/devices/platform/tegra_tmon/temp1_input;
aPaths[17]=/sys/kernel/debug/tegra_thermal/temp_tj;
aPaths[18]=/sys/devices/platform/s5p-tmu/temperature;
aPaths[19]=/sys/class/hwmon/hwmon3/device/temp1_input;
aPaths[20]=/sys/class/hwmon/hwmon0/device/temp1_input;
aPaths[21]=/sys/devices/virtual/thermal/thermal_zone1/temp;
aPaths[22]=/sys/devices/virtual/thermal/thermal_zone0/temp;
aPaths[23]=/sys/class/hwmon/hwmon1/device/temp1_input;
aPaths[24]=/sys/class/hwmon/hwmon2/device/temp1_input;




rm $outfile
touch $outfile

defaultTemperaturePath=-1

# Prints time, temperature & temperature path to an outfile
# $1 temperature  $2 deafultTemperaturePath
writeToOutFile () {
	temperature=$1
	echo $1
	temp=$(expr $1 / 1000)
	if [ temp -gt 1 ] || [ temp -eq 1 ]
		then
		temperature=$temp
	fi
	temperature_data=$(date +"%s")" "$temperature" "$2
	echo "$temperature_data" >> $outfile
}

# Main snippet of temperature data & path finder
while :
do
	if [ "$defaultTemperaturePath" != -1 ]
		then
		temperature=`cat $defaultTemperaturePath`
		writeToOutFile $temperature $defaultTemperaturePath
	else
		for path in ${aPaths[@]}
			do
			sensor=$(cat $path/type)
			temperature=$(cat $path/temp)
			cpuTemp=tsens_tz_sensor0 
				if [ "$sensor" == "$cpuTemp"  ]
				then
					if [ $temperature -ne 0 -o $temperature -ne -1 -o $temperature -eq 0 2>/dev/null ]
					then
						defaultTemperaturePath=$path/temp;
						echo $defaultTemperaturePath
						writeToOutFile $temperature $defaultTemperaturePath
						break
					fi
					break
				fi
			done
		fi
	sleep $sleepSeconds
done