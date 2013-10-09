package com.example.gpstracker;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

public class GPSChecker {

	/* FIELDS */

    private Activity activity;
    private Pass pass;
    private List<Resource> resourcesList;

    /* TYPES */

    public enum Resource {
            GPS
    }

    public static abstract class Pass {
            public abstract void pass();
    }

    /* API */

    public GPSChecker(Activity activity) {
            this.activity = activity;
    }

    public void check(Resource... resources) {
            resourcesList = Arrays.asList(resources);
            if (resourcesList.contains(Resource.GPS) && !isGPSActivated(activity)) {
                    new AlertDialog.Builder(activity).setMessage("GPS required.").setCancelable(false).setPositiveButton("GPS", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                    activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                    }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    activity.finish();
                            }
                    }).create().show();
            }else{
               pass.pass();
            }
    }

    public GPSChecker pass(Pass pass) {
            this.pass = pass;
            return this;
    }

    /* PRIVATE */

    private boolean isGPSActivated(Context context) {
            return ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
