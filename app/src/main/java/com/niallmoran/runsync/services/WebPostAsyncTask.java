package com.niallmoran.runsync.services;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.niallmoran.runsync.activities.RunSyncApplication;
import com.niallmoran.runsync.utilities.SamsungHealthHelper;
import com.niallmoran.runsync.utilities.WebPostHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebPostAsyncTask extends AsyncTask {

    private static SamsungHealthHelper helper;
    private static String url;

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.d("WebPostAsyncTask", "doInBackground");
        try {
            helper = (SamsungHealthHelper) objects[0];
            url = (String) objects[1];
            Log.d("WebPostAsyncTask", String.format("data read: %s", helper.recordCount));
            WebPostHelper poster = new WebPostHelper( url, helper.jsonString );
            poster.Post();
            return true;
        } catch (Exception e) {
            Log.e("WebPostAsyncTask", String.format("error: %s", e.getMessage()));
            return false;
        }
    }

    @Override
    protected void onPreExecute() {
        Log.d("WebPostAsyncTask", "onPreExecute");
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Object o) {
        Log.d("WebPostAsyncTask", "onPostExecute");
        boolean success = (Boolean)o;
        if(success) {
            Log.d("WebPostAsyncTask", String.format("JSON posted successfully"));
            SimpleDateFormat         sdfdate     = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat         sdftime     = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SharedPreferences        preferences = RunSyncApplication.getAppContext().getSharedPreferences("runsyncpreferences", RunSyncApplication.getAppContext().MODE_PRIVATE);
            SharedPreferences.Editor edit        = preferences.edit();
            edit.putString("datamessage", helper.recordCount + " records of data sent in the background on " +  sdfdate.format(new Date()) + " at " + sdftime.format(new Date()) );
            edit.commit();
        }
        else
            Log.d("WebPostAsyncTask", String.format("JSON failed to post"));
        super.onPostExecute(o);
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        Log.d("WebPostAsyncTask", "onProgressUpdate");
        super.onProgressUpdate(values);
    }

    @Override
    protected void onCancelled(Object o) {
        Log.d("WebPostAsyncTask", "onCancelled Object");
        super.onCancelled(o);
    }

    @Override
    protected void onCancelled() {
        Log.d("WebPostAsyncTask", "onCancelled");
        super.onCancelled();
    }
}
