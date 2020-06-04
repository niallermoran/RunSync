package com.niallmoran.runsync.utilities;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Use this class to help with posting data to a URL. Make sure to run within an AsynTask to keep it off the main thread
 *
 */
public class WebPostHelper  {

    private Exception ex= null;
    private static String APP_TAG = "RunSyncWebPostHelper";

    private String mUrl;
    private String mJson;

    public WebPostHelper(String url, String json)
    {
        this.mJson = json;
        this.mUrl = url;
    }

    public Boolean Post()
    {
        HttpURLConnection connection = null;
        try {

            Log.d( APP_TAG, String.format("Posting %s to URL: %s", mJson.substring(0,100) + " ....", mUrl   ) );

            URL uri = null;
            uri = new URL(mUrl);

            connection = (HttpURLConnection) uri.openConnection();
            connection.setRequestMethod( "POST" );
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = mJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            return true;
        }
        catch( ConnectException ce) {
            Log.e( APP_TAG, String.format("A connection error occured: %s", ce.getMessage()));
            return false;
        }
        catch( Exception e)
        {
            Log.e( APP_TAG, String.format("A general exception occured: %s: %s",e.getClass().toString() , e.getMessage() ));
            return false;
        }
        finally {
            if( connection != null)
                connection.disconnect();
        }

    }
}
