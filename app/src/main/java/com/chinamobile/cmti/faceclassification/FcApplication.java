package com.chinamobile.cmti.faceclassification;

import android.app.Application;
import android.util.Log;

import com.datami.smi.SdStateChangeListener;
import com.datami.smi.SmiResult;
        import com.datami.smi.SdState;
        import com.datami.smi.SmiSdk;

public class FcApplication extends Application implements
        SdStateChangeListener {

    private static final String mySdkKey = "dmi-att-hack-68fcfe5e708bfaa3806c4888912ea6f2ecb446fd";
    private static final String TAG = FcApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SmiSdk.getAppSDAuth(mySdkKey, this,  "", 0, true);
        } catch (Exception e) {
            Log.e(TAG, "failed to initialize app: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        // Application specific initializations
    }
    @Override
    public void onChange(SmiResult currentSmiResult) {
        SdState sdState = currentSmiResult.getSdState();
        Log.d(TAG, "sponsored data state : " + sdState);
        if(sdState == SdState.SD_AVAILABLE) {
            Log.d(TAG, "Using sponsored data!");
        } else if(sdState == SdState.SD_NOT_AVAILABLE) {
            Log.d(TAG, " - reason: " + currentSmiResult.getSdReason());
        } else if(sdState == SdState.WIFI) {
            Log.d(TAG, "Using wifi!");
        } }
}