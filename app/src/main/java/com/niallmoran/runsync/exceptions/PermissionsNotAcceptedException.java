package com.niallmoran.runsync.exceptions;

import android.content.res.Resources;

import com.niallmoran.runsync.R;

import static android.provider.Settings.System.getString;

public class PermissionsNotAcceptedException extends Exception {

    public PermissionsNotAcceptedException(String service) {
        super( String.format("Permissions not accepted in: %s", service));
    }
}
