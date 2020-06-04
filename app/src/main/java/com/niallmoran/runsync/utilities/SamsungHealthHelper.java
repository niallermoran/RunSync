package com.niallmoran.runsync.utilities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niallmoran.runsync.activities.MainActivity;
import com.niallmoran.runsync.activities.RunSyncApplication;
import com.niallmoran.runsync.exceptions.NotConnectedException;
import com.niallmoran.runsync.exceptions.PermissionsNotAcceptedException;
import com.niallmoran.runsync.models.ExerciseRecord;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SamsungHealthHelper<T extends ISamsungHealthContext> {

    // region local variables
    private ISamsungHealthContext mISamsungHealthContext = null;
    private HealthDataStore mStore;
    private HealthPermissionManager mPermissionsManager;
    private HealthConnectionErrorResult mConnError;
    private Set<HealthPermissionManager.PermissionKey> mKeySet;
    private Boolean mForceReconnect = false;
    private static HealthDataResolver mResolver;

    public int recordCount = 0;
    public String jsonString = null;
    public boolean isConnected = false;
    public boolean isPermissionReceived = false;
    public boolean isDataRead = false;

    public static final String STEP_SUMMARY_DATA_TYPE_NAME = "com.samsung.shealth.step_daily_trend";

    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
    public static final long ONE_YEAR = 365 * 24 * 60 * 60 * 1000;

    private static final String PROPERTY_TIME = "day_time";
    private static final String PROPERTY_COUNT = "count";
    private static final String PROPERTY_BINNING_DATA = "binning_data";
    private static final String ALIAS_TOTAL_COUNT = "count";
    private static final String ALIAS_DEVICE_UUID = "deviceuuid";
    private static final String ALIAS_BINNING_TIME = "binning_time";

    static final int EXERCISE_TYPE_WALKING = 1001;
    static final int EXERCISE_TYPE_RUNNING = 1002;
    // endregion

    public SamsungHealthHelper(ISamsungHealthContext context)
    {
        mISamsungHealthContext = context;
    }

    // region Connecting to Samsung Health


    /** Connects to Samsung Health, gets the correct permissions and extracts relevant health data.
     *
     */
    public void Connect()
    {
        // define the permissions set required to access data from Samsung Health
        mKeySet = new HashSet<HealthPermissionManager.PermissionKey>();
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));

        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(RunSyncApplication.getAppContext(), mConnectionListener);

        // create the permissions manager
        mPermissionsManager = new HealthPermissionManager(mStore);

        // create a resolver to get the data from the Samsung Health database
        mResolver = new HealthDataResolver(mStore, null);

        // connect to Samsung Health
        mStore.connectService();
    }

    /** The connection listener object is used to connect to Samsung Health
     *
     */
    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            isConnected = true;
            mISamsungHealthContext.OnSamsungConnected();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            isConnected = false;
            mISamsungHealthContext.OnSamsungConnectionFailed(error);
        }

        @Override
        public void onDisconnected() {
            isConnected = false;
            mISamsungHealthContext.OnSamsungDisconnected();
        }
    };

    // endregion


    // region Permissions

    /** Validates that permissions have been accepted by an end user via a layout
     *
     * @param forceConnect forces user to accept permissions even if they are already in place
     * @return true/false indicating if permissions have been accepted
     * @throws NotConnectedException
     */
    public void ValidatePermissions( boolean forceConnect, Activity activity ) throws NotConnectedException, InterruptedException {
        // make sure we are connected to Samsung Health
        if( this.isConnected )
        {
            if( arePermissionsAccepted() && !forceConnect ) {
                this.isPermissionReceived = true;
                mISamsungHealthContext.OnSamsungPermissionsAccepted();
            }
            else
            {
                // request permissions
                mPermissionsManager.requestPermissions(mKeySet, activity).setResultListener(mPermissionListener);
                //mPermissionListener.wait();
            }
        }
        else
            {
            throw new NotConnectedException(this.getClass().getName());
        }
    }


    /** checks if permissions have been set by the user
     *
     * @return true/false indicating if the relevant permissions have been accepted by the user
     */
    private boolean arePermissionsAccepted()
    {
        // Check whether the permissions that this application needs are acquired
        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = mPermissionsManager.isPermissionAcquired(mKeySet);

        // if the permissions map contains false then we don;t have what we need
        return !resultMap.containsValue(Boolean.FALSE);
    }

    /** Asks the user for permissions and lets the user know when they are received
     *
     */
    private final HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult>() {

                @Override
                public void onResult(HealthPermissionManager.PermissionResult result) {
                    Boolean accepted = arePermissionsAccepted();
                    isPermissionReceived = accepted;
                    if( accepted ) {
                        mISamsungHealthContext.OnSamsungPermissionsAccepted();
                    }
                    else {
                        mISamsungHealthContext.OnSamsungPermissionsRejected();
                    }
                }
            };

    // endregion


    // region Data Reading
    /// gets every exercise records from Samsung Health
    public void readExerciseRecords() throws NotConnectedException, PermissionsNotAcceptedException {

        if( !isConnected)
            throw new NotConnectedException(this.getClass().getName());
        else if (!arePermissionsAccepted() )
            throw new PermissionsNotAcceptedException(this.getClass().getName());
        else {

            long dayTime = TimeUtils.getStartTimeOfTodayInUtc();

            // Get sum of step counts by device
            HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                    .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                    .setSort(HealthConstants.Exercise.START_TIME, HealthDataResolver.SortOrder.DESC)
                    .build();

            try {
                mResolver.read(request).setResultListener(result -> {
                    int    totalCount = 0;
                    String json       = null;

                    try {
                        totalCount = result.getCount();

                        ObjectMapper mapper = new ObjectMapper();

                        Iterator<HealthData> iterator  = result.iterator();
                        ExerciseRecord[]     dataItems = new ExerciseRecord[totalCount];

                        int count = 0;
                        while (iterator.hasNext()) {
                            HealthData hData = iterator.next();
                            dataItems[count] = new ExerciseRecord(
                                    hData.getInt("duration"),
                                    hData.getLong("start_time"),
                                    hData.getInt("exercise_type"),
                                    hData.getLong("update_time"),
                                    hData.getLong("create_time"),
                                    hData.getFloat("max_speed"),
                                    hData.getFloat("distance"),
                                    hData.getFloat("calorie"),
                                    hData.getFloat("time_offset"),
                                    hData.getString("deviceuuid"),
                                    hData.getFloat("mean_speed"),
                                    hData.getLong("end_time"),
                                    hData.getString("datauuid")
                            );

                            count++;
                        }

                        json = mapper.writeValueAsString(dataItems);

                        this.recordCount = totalCount;
                        this.jsonString = json;
                        mISamsungHealthContext.OnSamsungDataRead();
                        isDataRead = true;

                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    } finally {
                        result.close();
                    }
                });
            } catch (Exception e) {
                Log.e(MainActivity.APP_TAG, "Getting exercise records failed.", e);
            }
        }
    }

    // endregion
}
