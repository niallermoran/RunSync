package com.niallmoran.runsync.activities;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import com.niallmoran.runsync.services.SamsungHealthBackgroundReceiver;

public class RunSyncApplication extends Application {

    private static Context mContext;
    private static AlarmManager mAlarmManager;
    private static String APP_TAG = "RunSyncApplication";
    private static SharedPreferences mActivityPreferences = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
        mAlarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        mActivityPreferences = RunSyncApplication.getAppContext().getSharedPreferences("runsyncpreferences", MODE_PRIVATE);

        String url = RunSyncApplication.GetUrl();
        if( url != null ) {
            RunSyncApplication.CreateSamsungSyncBackgroundTask(url);
            Log.d("RunSyncApplication","Background service started on application start");
        }
    }

    public static AlarmManager getAlarmManager()
    {
        return mAlarmManager;
    }

    /** creates a sSamsung Health background task to sync data with URL
     *
     * @param url
     */
    public static void CreateSamsungSyncBackgroundTask(String url )
    {
        try {
            Intent intent = new Intent(RunSyncApplication.getAppContext(), SamsungHealthBackgroundReceiver.class);
            intent.putExtra("url", url);
            PendingIntent sender = PendingIntent.getBroadcast(RunSyncApplication.getAppContext(), 123445324, intent, 0);
            RunSyncApplication.getAlarmManager().setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000 *60 *60, sender);//1 hour interval
            Log.d(APP_TAG, "Scheduled sync in background");
        }
        catch( Exception e) {
            Log.d(APP_TAG, String.format("Error: %s", e.getMessage()));
        }
    }

    /** stores URL so it will be remembered by the app
     *
     * @param url
     */
    public static void SetUrl( String url)
    {
        SharedPreferences.Editor edit= mActivityPreferences.edit();
        edit.putString("url", url);
        edit.commit();
    }

    /** gets the url stored for the app
     *
     * @return
     */
    public static String GetUrl()
    {
        if (mActivityPreferences.contains("url") )
        {
            return mActivityPreferences.getString("url", "");
        }
        else
            return null;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static Context getAppContext() {
        return mContext;
    }

}
