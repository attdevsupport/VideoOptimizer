/*
 *  Copyright 2014 AT&T
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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.AROLogger;

import java.util.List;

public class ARORadioMonitorService extends AROMonitorService {

    private static final String TAG = "ARORadioMonitorService";
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelphoneManager;

    /**
     * Setup and start monitoring
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand(...)");

        if (mAroUtils == null) {
            mAroUtils = new AROCollectorUtils();
            initFiles(intent);
            startARO_TraceMonitor();
        }
        return super.onStartCommand(intent, flags, startId);

    }

    /**
     * Starts the GPS peripherals trace collection
     */
    private void startARO_TraceMonitor() {
        Log.i(TAG, "startAROCameraTraceMonitor()");
        mTelphoneManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setARORadioSignalListener();
        mTelphoneManager.listen(mPhoneStateListener
                , PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

    }

    /**
     * Stops the trace collection
     */
    @Override
    protected void stopMonitor() {

        if (mPhoneStateListener != null) {
            mTelphoneManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mTelphoneManager = null;
            mPhoneStateListener = null;
        }
    }

    /**
     * Capture the device radio RSSI(signal strength) during the trace
     */
    private void setARORadioSignalListener() {

        mPhoneStateListener = new PhoneStateListener() {
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                // GSM Radio signal strength in integer value which will be converted to dDm (This is default considered network type)
                String mRadioSignalStrength = String.valueOf(0);

                //any 3gpp type SignalStrength
                if (mTelphoneManager != null && signalStrength.isGsm() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (mTelphoneManager.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                        mRadioSignalStrength = getLteSignalString(signalStrength, mRadioSignalStrength);
                    } else if ((mTelphoneManager.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_CDMA)) {
                        //If the network type is CDMA then look for CDMA signalstrength values.
                        mRadioSignalStrength = String.valueOf(signalStrength.getCdmaDbm());
                    } else if (mTelphoneManager.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0
                            || mTelphoneManager.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A) {
                        //If the network type is EVDO O/A then look for EVDO signal strength values.
                        mRadioSignalStrength = String.valueOf(signalStrength.getEvdoDbm());
                    }
                }

                AROLogger.v(TAG, "signal strength changed to " + mRadioSignalStrength);
                writeTraceLineToAROTraceFile(mRadioSignalStrength, true);
            }
        };
    }

    private String getLteSignalString(SignalStrength signalStrength, String mRadioSignalStrength) {
        int mLteSignalStrength = 0;
        int mLteRsrp = 0;
        int mLteRsrq = 0;
        int mLteRssnr = 0;
        int mLteCqi = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            List<CellSignalStrength> list = mTelphoneManager.getSignalStrength().getCellSignalStrengths();
            if (!list.isEmpty()) {
                for (CellSignalStrength cell : list) {
                    if (cell instanceof CellSignalStrengthLte) {
                        mLteSignalStrength = ((CellSignalStrengthLte) cell).getRssi();
                        AROLogger.d(TAG, "mLteSignalStrength : " + mLteSignalStrength);
                        mLteRsrp = ((CellSignalStrengthLte) cell).getRsrp();
                        AROLogger.d(TAG, "mLteRsrp : " + mLteRsrp);
                        mLteRsrq = ((CellSignalStrengthLte) cell).getRsrq();
                        AROLogger.d(TAG, "mLteRsrq : " + mLteRsrq);
                        mLteRssnr = ((CellSignalStrengthLte) cell).getRssnr();
                        AROLogger.d(TAG, "mLteRssnr : " + mLteRssnr);
                        mLteCqi = ((CellSignalStrengthLte) cell).getCqi();
                        AROLogger.d(TAG, "mLteCqi : " + mLteCqi);
                    }
                }
            }
        } else {
            try {
                mLteSignalStrength = Integer.parseInt(mAroUtils
                        .getSpecifiedFieldValues(SignalStrength.class, signalStrength,
                                "mLteSignalStrength"));
            } catch (NumberFormatException nmb) {
                AROLogger.e(TAG, "mLteSignalStrength not found in LTE Signal Strength");
            }

            try {
                mLteRsrp = Integer.parseInt(mAroUtils.getSpecifiedFieldValues(
                        SignalStrength.class, signalStrength, "mLteRsrp"));
            } catch (NumberFormatException nmb) {
                AROLogger.e(TAG, "mLteRsrp not found in LTE Signal Strength");
            }

            try {
                mLteRsrq = Integer.parseInt(mAroUtils.getSpecifiedFieldValues(
                        SignalStrength.class, signalStrength, "mLteRsrq"));
            } catch (NumberFormatException nmb) {
                AROLogger.e(TAG, "mLteRsrq not found in LTE Signal Strength");
            }
            try {
                mLteRssnr = Integer.parseInt(mAroUtils.getSpecifiedFieldValues(
                        SignalStrength.class, signalStrength, "mLteRssnr"));
            } catch (NumberFormatException nmb) {
                AROLogger.e(TAG, "mLteRssnr not found in LTE Signal Strength");
            }
            try {
                mLteCqi = Integer.parseInt(mAroUtils.getSpecifiedFieldValues(
                        SignalStrength.class, signalStrength, "mLteCqi"));
            } catch (NumberFormatException nmb) {
                AROLogger.e(TAG, "mLteCqi not found in LTE Signal Strength");
            }
        }
            // Check to see if LTE parameters are set
        if ((mLteSignalStrength == 0 && mLteRsrp == 0 && mLteRsrq == 0 && mLteCqi == 0)
                || (mLteSignalStrength == -1 && mLteRsrp == -1 && mLteRsrq == -1 && mLteCqi == -1)) {
            // No LTE parameters set. Use GSM signal strength
            final int gsmSignalStrength = signalStrength.getGsmSignalStrength();
            if (signalStrength.isGsm() && gsmSignalStrength != 99) {
                mRadioSignalStrength = String.valueOf(-113 + (gsmSignalStrength * 2));
            }
        } else {
            // If hidden LTE parameters were defined and not set to default values, then used them
            mRadioSignalStrength = mLteSignalStrength + " " + mLteRsrp + " " + mLteRsrq
                    + " " + mLteRssnr + " " + mLteCqi;
        }
        return mRadioSignalStrength;
    }

}





