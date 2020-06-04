package com.niallmoran.runsync.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.niallmoran.runsync.activities.RunSyncApplication;
import com.niallmoran.runsync.exceptions.NotConnectedException;
import com.niallmoran.runsync.exceptions.PermissionsNotAcceptedException;
import com.niallmoran.runsync.utilities.ISamsungHealthContext;
import com.niallmoran.runsync.utilities.SamsungHealthHelper;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;

public class SamsungHealthBackgroundReceiver extends BroadcastReceiver implements ISamsungHealthContext {

    private SamsungHealthHelper mSamsungHelper = null;
    private String mUrl = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        if( intent !=null && intent.getAction() !=null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d("RunSyncBackService", String.format("loaded on boot"));
        }

        try {
            mUrl = intent.getStringExtra("url");

            // connect
            mSamsungHelper = new SamsungHealthHelper(this);
            mSamsungHelper.Connect();

            Log.d("RunSyncBackService", String.format("started command for url: %s", mUrl));
        }
        catch( Exception e) {
            Log.e("RunSyncBackService", String.format("error: %s", e.getMessage()));
        }
    }

    @Override
    public void OnSamsungConnected() {
        Log.d("RunSyncBackService", String.format("connected"));
        try {
            mSamsungHelper.readExerciseRecords();
        } catch (NotConnectedException e) {
            Log.e("RunSyncBackService", String.format("error: %s", e.getMessage()));
        } catch (PermissionsNotAcceptedException e) {
            Log.e("RunSyncBackService", String.format("error: %s", e.getMessage()));
        }
    }

    @Override
    public void OnSamsungConnectionFailed(HealthConnectionErrorResult error) {
        Log.d("RunSyncBackService", String.format("failed: %s", error));
    }

    @Override
    public void OnSamsungDisconnected() {
        Log.d("RunSyncBackService", String.format("disconnected"));

    }

    @Override
    public void OnSamsungPermissionsAccepted() {
        return;
    }

    @Override
    public void OnSamsungPermissionsRejected() {
        return;
    }

    @Override
    public void OnSamsungDataRead() {

        try {
            WebPostAsyncTask task = new WebPostAsyncTask();
            task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, mSamsungHelper, mUrl);
        } catch (Exception e) {
            Log.e("RunSyncBackService", String.format("error: %s", e.getMessage()));
        }
    }

}
