package com.niallmoran.runsync.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.microsoft.appcenter.analytics.Analytics;
import com.niallmoran.runsync.activities.RunSyncApplication;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if( intent !=null && intent.getAction() !=null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d("BootReceiver", "loaded on boot");

            Analytics.trackEvent("loaded on boot");

            String url = RunSyncApplication.GetUrl();
            if( url != null)
            {
                RunSyncApplication.CreateSamsungSyncBackgroundTask(url);

                Log.d("BootReceiver", String.format("created background task after boot for %s", url));
            }
        }
    }
}
