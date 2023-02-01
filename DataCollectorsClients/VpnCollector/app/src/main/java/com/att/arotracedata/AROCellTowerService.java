package com.att.arotracedata;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.att.arocollector.utils.AROCollectorUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AROCellTowerService extends AROMonitorService {

    private static final String TAG = "RadioCellTowerMonitor";
    private TelephonyManager mTelphoneManager;
    private PhoneStateListener mPhoneStateListener;
    private List<String> cellList = new ArrayList<>();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mAroUtils == null) {
            mAroUtils = new AROCollectorUtils();
            initFiles(intent);
            mTelphoneManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String serviceName = mTelphoneManager.getSimOperatorName();
            Log.i(TAG, "Service name: " + serviceName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
                    @Override
                    public void onCellInfo(List<CellInfo> cellInfo) {
                        for (CellInfo cellInfoEach : cellInfo) {
                            if (cellInfoEach instanceof CellInfoLte) {
                                CellIdentityLte cellIdentityLte = ((CellInfoLte) cellInfoEach).getCellIdentity();
                                if (cellIdentityLte.getCi() != 0 && cellIdentityLte.getCi() != Integer.MAX_VALUE) {
                                    Log.i(TAG, "cell ID: " + cellIdentityLte.getCi());
                                    cellList.add(mTelphoneManager.getSimOperatorName() + " " + (long) cellIdentityLte.getCi());
                                }
                            } else if (cellInfoEach instanceof CellInfoNr) {
                                CellIdentityNr cellIdentityNr = (CellIdentityNr) ((CellInfoNr) cellInfoEach).getCellIdentity();
                                if (cellIdentityNr.getNci() != 0 && cellIdentityNr.getNci() != Integer.MAX_VALUE) {
                                    Log.i(TAG, "cell ID: " + cellIdentityNr.getNci());
                                    cellList.add(mTelphoneManager.getSimOperatorName() + " " + cellIdentityNr.getNci());
                                }
                            }
                        }
                    }
                };
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mTelphoneManager.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);
                }
            }

            mTelphoneManager.listen(new PhoneStateListener() {
                                        @Override
                                        public void onCellInfoChanged(List<CellInfo> cellInfo) {
                                            super.onCellInfoChanged(cellInfo);
                                            for (CellInfo cellInfoEach : cellInfo) {
                                                if (cellInfoEach instanceof CellInfoLte) {
                                                    CellIdentityLte cellIdentityLte = ((CellInfoLte) cellInfoEach).getCellIdentity();
                                                    if (cellIdentityLte.getCi() != 0 && cellIdentityLte.getCi() != Integer.MAX_VALUE) {
                                                        if(!cellList.contains(mTelphoneManager.getSimOperatorName() + " " + (long) cellIdentityLte.getCi())){
                                                            Log.i(TAG, "cell ID: " + cellIdentityLte.getCi());
                                                            cellList.add(mTelphoneManager.getSimOperatorName() + " " + (long) cellIdentityLte.getCi());
                                                        }
                                                    }
                                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    if (cellInfoEach instanceof CellInfoNr) {
                                                        CellIdentityNr cellIdentityNr = (CellIdentityNr) ((CellInfoNr) cellInfoEach).getCellIdentity();
                                                        if (cellIdentityNr.getNci() != 0 && cellIdentityNr.getNci() != Integer.MAX_VALUE) {
                                                            if(!cellList.contains(mTelphoneManager.getSimOperatorName() + " " + cellIdentityNr.getNci())) {
                                                                Log.i(TAG, "cell ID: " + cellIdentityNr.getNci());
                                                                cellList.add(mTelphoneManager.getSimOperatorName() + " " + cellIdentityNr.getNci());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                    , PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                            | PhoneStateListener.LISTEN_CALL_STATE
                            | PhoneStateListener.LISTEN_CELL_INFO
                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        }
        return super.onStartCommand(intent, flags, startId);
    }


    protected void initFiles(Intent intent) {

        if (intent != null) {

            Log.i(TAG, "initFiles(Intent " + intent.toString() + ") hasExtras = " + intent.getExtras());
            String traceDirStr = intent.getStringExtra("TRACE_DIR");
            traceFileName = intent.getStringExtra("TRACE_FILE_NAME");

            traceDir = new File(traceDirStr);
            traceDir.mkdir();

            try {
                outputTraceFile = traceDir + "/" + traceFileName;
                outputTraceFileStream = new FileOutputStream(outputTraceFile, true);
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputTraceFileStream));
            } catch (FileNotFoundException e) {
                outputTraceFileStream = null;
            }

        } else {
            Log.i(TAG, "intent is null");
        }
    }

    @Override
    protected void stopMonitor() {
        for (String cellID : cellList) {
                writeTraceLineToAROTraceFile(cellID, false);
        }
    }
}
