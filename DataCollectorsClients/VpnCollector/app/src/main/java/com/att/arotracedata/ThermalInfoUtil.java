/*
 *  Copyright 2021 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.att.arotracedata;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class ThermalInfoUtil {
    public static final String TAG = ThermalInfoUtil.class.getSimpleName();
    private BufferedWriter bw = null;
    private FileWriter fw = null;
    private CPUTemperatures cpuTemperatures = null;
    private ExecutorService executor;
    private ThermalCallBack LocalCallback;
    private String[] thermalPath = new String[]{
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
            "/sys/devices/platform/s5p-tmu/curr_temp",
            "/sys/devices/platform/s5p-tmu/temperature",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
            "/sys/devices/platform/tegra_tmon/temp1_input",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/ext_temperature",
            "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
            "/sys/devices/platform/tegra-tsensor/tsensor_temperature",
            "/sys/devices/platform/tegra_tmon/temp1_input",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/class/hwmon/hwmon0/device/temp1_input",
            "/sys/class/hwmon/hwmon0/temp1_input",
            "/sys/class/hwmon/hwmon1/device/temp1_input",
            "/sys/class/hwmon/hwmon2/device/temp1_input",
            "/sys/class/hwmon/hwmon3/device/temp1_input",
            "/sys/class/hwmon/hwmonX/temp1_input",
            "/sys/htc/cpu_temp"
    };


    public ThermalInfoUtil(ExecutorService executor) {
        this.executor = executor;
        init();
    }

    public void startCPUTemp(ThermalCallBack callback, Handler resultHandler) {
        if (cpuTemperatures == null) {
            cpuTemperatures = new CPUTemperatures(callback, resultHandler);
        }
        executor.execute(cpuTemperatures);
    }


    /*
     * Initiated a file for recording attenuation throttle download and upload activity
     */
    public void init() {
        String logfile = "temperature_data";
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/ARO");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, logfile);
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage(), ioe);
        }
    }

    private void notifyResult(ResultCPUThermal result, ThermalCallBack callback, Handler resultHandler) {
        resultHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.callbackResult(result);
            }
        });
    }

    public Double getValueFromFile(String path) {
        String str;
        try {
            File file = new File(path);
            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            str = bufferedReader.readLine();
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            Double value;
            if (str == null) {
                return null;
            }
            try {
                value = Double.parseDouble(str);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, nfe.getMessage(), nfe);
                value = null;
            }
            return value;
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, fnfe.getMessage(), fnfe);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private void writeToFile(double thermal, String path) {
        try {
            long time = ((new Date()).getTime()) / 1000;
            bw.write(time + " " + Math.round(thermal) + " " + path);
            Log.i(TAG, time + " " + thermal + " " + path);
            bw.newLine();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    public void closeFile() {
        try {
            Log.i(TAG, "close the buffer writer");
            if (bw != null)
                bw.close();
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage(), ioe);
        }

    }

    private boolean checkThermalValid(Double readValue) {
        // value is celsius
        return (readValue != null) && (readValue >= 0.00) && (readValue <= 100.00);
    }

    public interface ThermalCallBack {
        void callbackResult(ResultCPUThermal result);
    }

    public static class ResultCPUThermal {
        String filePath = null;
        boolean flag;
        double thermalLocal;

        public String getFilePath() {
            return filePath;
        }

        public boolean isFlag() {
            return flag;
        }

        public double getThermalLocal() {
            return thermalLocal;
        }

    }

    private class CPUTemperatures implements Runnable {

        private ThermalCallBack mCallback;
        private ResultCPUThermal cachedResult; // remember
        private Handler mHandler;

        public CPUTemperatures(ThermalCallBack callback, Handler resultHandler) {
            this.mCallback = callback;
            this.mHandler = resultHandler;
        }

        @Override
        public void run() {
            Double thermalValue = null;
            String cpuFilePath = null;
            boolean flagDivide = false;
            ResultCPUThermal result = null;

            if (cachedResult != null) {

                thermalValue = getValueFromFile(cachedResult.filePath);

                if (thermalValue != null) {
                    if (checkThermalValid(thermalValue)) {
                        writeToFile(thermalValue, cachedResult.filePath);
                    } else if (checkThermalValid(thermalValue / 1000)) {
                        thermalValue = thermalValue / 1000;
                        writeToFile(thermalValue, cachedResult.filePath);
                    }
                    cachedResult.thermalLocal = thermalValue.doubleValue();
                }
                if (mCallback != null) {
                    notifyResult(cachedResult, mCallback, mHandler);
                }

            } else {
                for (String path : thermalPath) {
                    thermalValue = getValueFromFile(path);
                    if (thermalValue != null) {
                        if (checkThermalValid(thermalValue)) {
                            cpuFilePath = path;
                            flagDivide = false;
                            Log.d(TAG, thermalValue + "in path: " + cpuFilePath);
                            break;
                        } else if (checkThermalValid(thermalValue / 1000)) {
                            thermalValue = thermalValue / 1000L;
                            cpuFilePath = path;
                            flagDivide = true;
                            Log.d(TAG, thermalValue + "in path: " + cpuFilePath);
                            break;
                        }
                    } else {
                        Log.d(TAG, "Unable to access temp in path: " + path);
                    }
                }
                if (thermalValue == null) {
                    Log.d(TAG, "Unable to find the thermal path!");
                    notifyResult(result, mCallback, mHandler);
                }
            }

            if (cpuFilePath != null && mCallback != null && thermalValue != null) {
                result = new ResultCPUThermal();
                result.thermalLocal = thermalValue;
                result.filePath = cpuFilePath;
                result.flag = flagDivide;
                cachedResult = result;
                writeToFile(thermalValue, result.filePath);
                notifyResult(result, mCallback, mHandler);
            }

        }

    }

}
