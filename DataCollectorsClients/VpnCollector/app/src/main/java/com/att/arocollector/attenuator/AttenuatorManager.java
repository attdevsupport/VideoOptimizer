/*
 *  Copyright 2018 AT&T
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
package com.att.arocollector.attenuator;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  AttenuatorManager.java provides centralized control of download and upload throttle number
 */

public class AttenuatorManager {
    private static final String TAG = AttenuatorManager.class.getSimpleName();
    private static final String TAG_DL = "DL";
    private static final String TAG_UL = "UL";
    private static final String TAG_DLT =  "DLT"; //download throughput
    private static final String TAG_ULT = "ULT"; //upload throughput

    private BufferedWriter bw = null;
    private FileWriter fw = null;
    private BufferedWriter bws = null;
    private FileWriter fws = null;
    private static AttenuatorManager amInstance = new AttenuatorManager();

    //Creates a new AtomicInteger with initial value 0.
    private AtomicInteger delayDLAtomic = new AtomicInteger();
    private AtomicInteger delayULAtomic = new AtomicInteger();

    //initial number for speed is 100Mbps
    private AtomicInteger throttleDLAtomic = new AtomicInteger(AttenuatorUtil.DEFAULT_THROTTLE_SPEED);
    private AtomicInteger throttleULAtomic = new AtomicInteger(AttenuatorUtil.DEFAULT_THROTTLE_SPEED);

    private AttenuatorManager() {}

    public static AttenuatorManager getInstance() {

        return amInstance;
    }

    public int getThrottleDL() {
        Log.i(TAG, "Download throttle get: "+throttleDLAtomic.get()+ " kbps");
        return throttleDLAtomic.get();
    }

    public void setThrottleDL(int throttleDL) {
        throttleDLAtomic.set(throttleDL);
        printThroughputLog(throttleDL,TAG_DLT);
        Log.i(TAG, "Download throttle set: "+throttleDL+ " kbps");

    }
    public int getThrottleUL() {
        return throttleULAtomic.get();
    }

    public void setThrottleUL(int throttleUL) {
        throttleULAtomic.set(throttleUL);
        printThroughputLog(throttleUL, TAG_ULT);
        Log.i(TAG, "Upload throttle set: " + throttleUL + " kbps");

    }

     public int getDelayDl() {
        Log.i(TAG, "Download delay get: "+ delayDLAtomic.get()+ " ms");
        return delayDLAtomic.get();
    }

    public void setDelayDl(int delayDl) {
        delayDLAtomic.set(delayDl);
        try {
            printDelayLog(delayDl, TAG_DL);
        } catch (FileNotFoundException e) {
            Log.e(TAG,e.getMessage(),e);
        }
        Log.i(TAG, "Download delay set: "+delayDl+ " ms");

    }

    public int getDelayUl() {
        Log.i(TAG, "Upload delay get: "+ delayULAtomic.get()+ " ms");
        return delayULAtomic.get();
    }

    public void setDelayUl(int delayUl) {
        delayULAtomic.set(delayUl);
        try {
            printDelayLog(delayUl, TAG_UL);
        } catch (FileNotFoundException e) {
            Log.e(TAG,e.getMessage(),e);
        }
        Log.i(TAG, "Upload delay set: " + delayUl + " ms");
    }

    /*
     * Initiated a file for recording attenuation throttle download and upload activity
     */
    public void init() {
        String logfile = "attenuation_logs";
        String speedFile = "speedthrottle_logs";
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/ARO");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, logfile);
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);

            File fileLog = new File(dir,speedFile);
            fws = new FileWriter(fileLog);
            bws = new BufferedWriter(fws);

        } catch (IOException ioe) {
            Log.e(TAG,ioe.getMessage(),ioe);
        }
    }

    private void printDelayLog(int delayTime, String direction)
            throws FileNotFoundException {
        try {
            long time = (new Date()).getTime();
            bw.write(direction + " , " + delayTime + " , " + time);
            Log.i(TAG,direction + " , " + delayTime + " , " + time);
            bw.newLine();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage(),e);
        }

    }

    private void printThroughputLog(int throughput, String direction){
        try {
            long time = (new Date()).getTime();
            bws.write(direction + " , " + throughput + " , " + time);
            Log.i(TAG,direction + " , " + throughput + " , " + time);
            bws.newLine();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage(),e);
        }

    }
    public void terminateDelayLog() {
        //for draw the step chart, we need to have at least two points for drawing.
        setEndPointforChart();
        try {
            if (bw != null)
                bw.close();
        } catch (IOException ioe) {
            Log.e(TAG,ioe.getMessage(),ioe);
        }
    }

    public void terminateThroughputLog(){
        setEndPointforThroughputChart();
        try {
            if(bws !=null)
                bws.close();
        }catch(IOException ioe){
            Log.e(TAG,ioe.getMessage(),ioe);
        }
    }

    private void setEndPointforThroughputChart(){
        int finalThroughputDL = getThrottleDL();
        setThrottleDL(finalThroughputDL);
        int finalThourghputUL = getThrottleUL();
        setThrottleUL(finalThourghputUL);
    }

    private void setEndPointforChart(){
        int finalDLTime = getDelayDl();
        setDelayDl(finalDLTime);
        int finalULTime = getDelayUl();
        setDelayUl(finalULTime);
    }
 }
