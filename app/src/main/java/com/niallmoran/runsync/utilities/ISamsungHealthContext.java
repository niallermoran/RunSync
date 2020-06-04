package com.niallmoran.runsync.utilities;

import android.app.Activity;
import android.content.Context;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;

public interface ISamsungHealthContext {
    void OnSamsungConnected();
    void OnSamsungConnectionFailed(HealthConnectionErrorResult error);
    void OnSamsungDisconnected();
    void OnSamsungPermissionsAccepted();
    void OnSamsungPermissionsRejected();
    void OnSamsungDataRead();
}
