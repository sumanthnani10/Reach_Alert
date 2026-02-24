package com.ismartapps.reachalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class StopRing extends BroadcastReceiver {

    String TAG = "StopRing";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Stopping Ring");
        Utils.stopRing(context);
        Utils.clearNotifications();
    }
}
