package com.ismartapps.reachalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FullScreenIntent extends AppCompatActivity {

    String TAG = "StopRing";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        hideNavigationBar();
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = settings.getBoolean("dark",false);
        if(dark)
            setContentView(R.layout.fullscreenintent_dark);
        else{
            setContentView(R.layout.fullscreenintent);
        }
        TextView stop = findViewById(R.id.stop);
        TextView text = findViewById(R.id.location_name);
        Context context = this;
        Log.d(TAG, "onReceive: Stopping Ring");
        String placeName = getIntent().getStringExtra("location_name");
        text.setText(placeName);
        stop.setOnClickListener(view -> {
            Utils.stopRing(context);
            Utils.clearNotifications();
            finish();
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Utils.stopRing(context);
                Utils.clearNotifications();
                finish();
            }
        },5*60*1000);

    }

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_DOWN:

            case KeyEvent.KEYCODE_VOLUME_UP:
                Utils.stopRing(this);
                Utils.clearNotifications();
                finish();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }*/
}
