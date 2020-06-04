package com.niallmoran.runsync.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.niallmoran.runsync.BuildConfig;
import com.niallmoran.runsync.R;
import com.niallmoran.runsync.exceptions.NotConnectedException;
import com.niallmoran.runsync.exceptions.PermissionsNotAcceptedException;
import com.niallmoran.runsync.utilities.ISamsungHealthContext;
import com.niallmoran.runsync.utilities.SamsungHealthHelper;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ISamsungHealthContext {

    // region Local Variables
    public static final String APP_TAG = "RunSyncMainActivity";
    public static final int MSG_UNCOLOR_START = 0;
    public static final int MSG_UNCOLOR_STOP = 1;
    public static final int MSG_COLOR_START = 2;
    public static final int MSG_COLOR_STOP = 3;
    public static final String MESSENGER_INTENT_KEY = BuildConfig.APPLICATION_ID + ".MESSENGER_INTENT_KEY";
    public static final String WORK_DURATION_KEY = BuildConfig.APPLICATION_ID + ".WORK_DURATION_KEY";
    private TextView ctlMessage = null;
    private TextView ctlMessagePosted = null;
    private TextView ctlMessageCount = null;
    private EditText ctlUriText = null;
    private Button ctlButton = null;

    private MainActivity mInstance = null;
    private SamsungHealthHelper mSamsungHelper = null;
    private HealthDataStore mStore;
    private HealthPermissionManager mPermissionsManager;
    private HealthConnectionErrorResult mConnError;
    private Set<PermissionKey> mKeySet;
    private boolean mIsSamsungHealthConnected = false;
    private SharedPreferences mActivityPreferences = null;

    // create a task to regularly update controls as background services may update variables
    private static UIUpdateTask mUiTask;

    // endregion

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get setup for access to Samsung Health
        mInstance = this;
        ctlMessage = findViewById(R.id.message);
        ctlMessageCount = findViewById(R.id.messageCount);
        ctlButton = findViewById(R.id.button);
        ctlUriText = findViewById(R.id.txtUrl);
        ctlMessagePosted = findViewById(R.id.msgData);

        // see if a url was already set
        ctlUriText.setText( RunSyncApplication.GetUrl());

        // connect to relevant data source
        mSamsungHelper = new SamsungHealthHelper(this);
        try {
            // connect
            mSamsungHelper.Connect();
        }
        catch( Exception e)
        {
            displayException(e);
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();



        // provide custom configuration
        Configuration myConfig = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();

        //initialize WorkManager
        try {
            WorkManager.initialize(this, myConfig);
        }
        catch( RuntimeException re)
        {
       //     displayException(re);
        }

        ctlUriText.setOnKeyListener(new EditText.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if( isURIValid())
                {
                    RunSyncApplication.SetUrl( ctlUriText.getText().toString());
                }

                updateControls();

                return true;
            }
        });

        // start the thread to keep the UI updated
        mUiTask = new UIUpdateTask();
        mUiTask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR);
    }

// region UI manipulation

    /** Shows errors if Samsung Health cannot connect
     *
     * @param error
     */
    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        mConnError = error;
        String message = "Connection with Samsung Health is not available";

        if (mConnError.hasResolution()) {
            switch(error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    message = "Please install Samsung Health";
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    message = "Please upgrade Samsung Health";
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    message = "Please enable Samsung Health";
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    message = "Please agree with Samsung Health policy";
                    break;
                default:
                    message = "Please make Samsung Health available";
                    break;
            }
        }

        alert.setMessage(message);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (mConnError.hasResolution()) {
                    mConnError.resolve(mInstance);
                }
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton("Cancel", null);
        }

        alert.show();
    }

    /** clear the url text field
     *
     * @param view
     */
    public void onClickClearButton(View view) {
        ctlUriText.setText("");
    }

    /** updates controls on the screen based on constraints to push data, e.g. connection to Samsung Health, Required Permissions being set and a URl defined
     *
     */
    private void updateControls() {

            // update the records count message
            ctlMessageCount.setText( String.format( "%s %s",  Integer.toString( mSamsungHelper.recordCount ), getString( R.string.msgActivitiesCount) )  );
            ctlMessageCount.setVisibility(View.VISIBLE);

            // enable or disable the sync button
            ctlButton.setEnabled( mSamsungHelper.isConnected && mSamsungHelper.isPermissionReceived && isURIValid()  );

            // update the connection message for the user
            if( !mSamsungHelper.isConnected )
                ctlMessage.setText(R.string.msg_disconnected);
            else if( mSamsungHelper.isConnected && !mSamsungHelper.isPermissionReceived )
                ctlMessage.setText(R.string.msg_connected_permissions_notacquired);
            else if ( mSamsungHelper.isConnected && mSamsungHelper.isPermissionReceived )
                ctlMessage.setText(R.string.msg_connected_permissions_acquired);

            // get the last time data was synced message
            String datasentmessage = getDataSentMessage();
            ctlMessagePosted.setText( datasentmessage );
    }


    /** Displays and logs information for the user
     */
    private void displayUserMessage(String message)
    {
        Toast.makeText(getApplicationContext(),message, Toast.LENGTH_LONG).show();
    }

    /** displays an exception
     *
     * @param e the exception to display
     */
    private void displayException( Exception e)
    {
        this.displayUserMessage(e.getMessage());
    }

    /** checks if the URL defined in the TextEdit control is valid
     *
     * @return true/false of URL is valid
     */
    private boolean isURIValid()
    {
        String url = ctlUriText.getText().toString();
        try {
            URL uri = new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }


    /**
     * Runs a background thread to sync the json data with the defined URL
     * @param view
     */
    public void onClickSyncButton(View view) {
        String  url = ctlUriText.getText().toString();
        ctlButton.setEnabled(false);

        // set the task to run on a schedule in the background
        RunSyncApplication.CreateSamsungSyncBackgroundTask(url);

        displayUserMessage("Data will be synced every hour");

        updateControls();
    }

    //endregion



    @Override
    public void onDestroy() {
        mStore.disconnectService();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {

        if (item.getItemId() == R.id.connectsamsung) {
            try {
                mSamsungHelper.ValidatePermissions(true, this);
            } catch (NotConnectedException e) {
                displayException(e);
            } catch (InterruptedException e) {
                displayException(e);
            }
        }

        return true;
    }

    @Override
    public void OnSamsungConnected() {

        updateControls();

        // get permissions
        try {
            mSamsungHelper.ValidatePermissions( false, this);
        } catch (NotConnectedException e) {
            displayException(e);
        } catch (InterruptedException e) {
            displayException(e);
        }
    }

    @Override
    public void OnSamsungConnectionFailed(HealthConnectionErrorResult error) {
        updateControls();
    }

    @Override
    public void OnSamsungDisconnected() {
        updateControls();
    }

    /** Fires when permissions have been accepted
     *
     */
    @Override
    public void OnSamsungPermissionsAccepted() {
        try {
            mSamsungHelper.readExerciseRecords();
        } catch (NotConnectedException e) {
            displayException(e);
        } catch (PermissionsNotAcceptedException e) {
            displayException(e);
        }
    }

    /** let the user know and try again as permissions must be granted to continue
     *
     */
    @Override
    public void OnSamsungPermissionsRejected() {
        displayUserMessage(getString(R.string.msg_perm_acquired));
    }

    @Override
    public void OnSamsungDataRead() {
        updateControls();
    }


// region Store Data in Application Context

    /** gets the data sent message from the last time the sync was completed
     *
     * @return get's the message to display on screen
     */
    private String getDataSentMessage()
    {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("runsyncpreferences", MODE_PRIVATE);
        if (preferences.contains("datamessage") )
        {
             return preferences.getString("datamessage", "") ;
        }
        else
            return "";
    }

    /** updates the amount of data sent message after sync is complete
     *
      */
    private void setDataSentMessage()
    {
        SimpleDateFormat sdfdate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdftime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        SharedPreferences preferences = getApplicationContext().getSharedPreferences("runsyncpreferences", MODE_PRIVATE);
        SharedPreferences.Editor edit= preferences.edit();
        edit.putString("datamessage", mSamsungHelper.recordCount + " records of data sent on " +  sdfdate.format(new Date()) + " at " + sdftime.format(new Date()) );
        edit.commit();
    }
// endregion

    /** A class to update the UI controls on a async thread.
     *
     */
    private class UIUpdateTask extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            // just wait a few seconds, execution is in the onPostExecute as it needs to update the UI thread
            try {
                while(true) {
                    TimeUnit.SECONDS.sleep(10);
                    publishProgress();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Log.d("RunSyncMainActivity", "updating UI");
            updateControls();
            super.onProgressUpdate(values);
        }
    }

}
