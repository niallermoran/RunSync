package com.niallmoran.runsync.exceptions;

import android.app.Application;
import android.content.ContentResolver;
import android.content.res.Resources;

import com.niallmoran.runsync.R;

import static android.provider.Settings.System.getString;

public class NotConnectedException extends Exception {

    public NotConnectedException(String service) {
        super( String.format("Samsung Health not connected: %s", service));
    }
}
