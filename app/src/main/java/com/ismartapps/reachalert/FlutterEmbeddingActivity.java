package com.ismartapps.reachalert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class FlutterEmbeddingActivity extends FlutterActivity {

    static final String CHANNEL = "com.confegure.reach_alert/methods";
    String TAG = "FlutterEmbeddingActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        try {
            switch (call.method) {
                case "locationPermissionResponse":
                    Intent intent = getIntent();
                    android.util.Log.d(TAG, "onMethodCall: ");
                    boolean resp = call.argument("accepted");
                    Log.d(TAG, "onMethodCall: "+resp);
                    intent.putExtra("accepted", resp);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                    break;
            }
        } catch (Exception e) {
            result.error(null, e.getMessage(), null);
        }
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(this::onMethodCall);
    }
}
