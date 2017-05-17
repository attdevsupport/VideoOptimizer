/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.Service;
import android.content.Intent;
import android.nfc.Tag;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.att.arocollector.utils.AROCollectorUtils;

public class AROCpuTempService extends Service {

    public static final String TAG = "AROCpuTempService";

    /** The root directory of the ARO Data Collector Trace. */
    public static final String ARO_TRACE_ROOTDIR = "/sdcard/ARO/";

    private static final String killTempPayloadFileName = "killcputemperature.sh";

    private static final String payloadFileName = "cputemperature.sh";

    private static final String tempLogFileName = "temperature_data";

    private static final String remoteExecutable = ARO_TRACE_ROOTDIR + payloadFileName;

    private Thread scriptThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (initFiles()) {
            startAROCpuTempTrace();
        } else {
            Log.e(TAG, "Cannot start ARO CPU temperature capture because files could not be initialized.");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean initFiles() {
        if (!isTraceDirExist(ARO_TRACE_ROOTDIR)) {
            createTraceRootDirectory();
        }
        cleanOldTempLogFile();
        return copyScript();
    }

    private boolean isTraceDirExist(String dir) {
        File file = new File(dir);
        return file.exists();
    }

    private void createTraceRootDirectory() {
        try {
            Runtime.getRuntime().exec("mkdir " + ARO_TRACE_ROOTDIR);
        } catch(Exception e) {
            Log.e(TAG, "Exception occurs when creating ARO trace root directory. :: " + e.getMessage());
        }
    }

    private void cleanOldTempLogFile() {
        try {
            Runtime.getRuntime().exec("rm " + ARO_TRACE_ROOTDIR + tempLogFileName);
        } catch(Exception e) {
            Log.e(TAG, "Exception occurs when cleaning old temperature log file. :: " + e.getMessage());
        }
    }

    private boolean copyScript() {
        try {
            InputStream in = readScriptFromResource();
            writeScript(in, remoteExecutable);
        } catch(Exception e) {
            Log.e(TAG, "Exception occurs when copying script. :: " + e.getMessage());
            return false;
        }
        return true;
    }

    private InputStream readScriptFromResource() {
        InputStream in = getResources().openRawResource(
                        getResources().getIdentifier("cputemperature", "raw", getPackageName()));
        return in;
    }

    private void writeScript(InputStream in, String fileName) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(fileName));
            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private void startAROCpuTempTrace() {
        final String cmd = "sh " + remoteExecutable
                         + " " + ARO_TRACE_ROOTDIR
                         + " " + tempLogFileName
                         + " " + killTempPayloadFileName;
        scriptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process proc = Runtime.getRuntime().exec(cmd);
                    if (proc != null) {
                        printError(proc);
                    }
                } catch(Exception e) {
                    Log.e(TAG, "Exception occurs when collecting CPU temperature. :: " + e.getMessage());
                }
            }
        });
        scriptThread.start();
    }

    public void stopAROCpuTempTrace() {
        final String cmd = "sh " + ARO_TRACE_ROOTDIR + killTempPayloadFileName;
        try {
            if (scriptThread != null) {
                scriptThread.interrupt();
            }
            Process proc = Runtime.getRuntime().exec(cmd);
            if (proc != null) {
                printError(proc);
            }
        } catch(Exception e) {
            Log.e(TAG, "Exception occurs when stopping CPU temperature. :: " + e.getMessage());
        }
    }

    private void printError(Process proc) throws IOException {
        Log.i(TAG, "printing script error if any...");
        InputStreamReader reader = null;
        BufferedReader error = null;
        try {
            reader = new InputStreamReader(proc.getErrorStream());
            error = new BufferedReader(reader);
            String line;
            while((line = error.readLine()) != null) {
                Log.i(TAG, line);
            }
        } finally {
            if (error != null) {
                error.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        stopAROCpuTempTrace();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
