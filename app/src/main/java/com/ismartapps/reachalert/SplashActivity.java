package com.ismartapps.reachalert;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplashActivity extends Activity {
    private static final String TAG = "SA";
    private Intent intent, mainIntent;
    private boolean fromShare;
    private LatLng latLng;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.setContentView(R.layout.splash_activity);
        ImageView imageView = findViewById(R.id.splash_img);
        ImageView shadow = findViewById(R.id.splash_shadow);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        imageView.startAnimation(animation);

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            shadow.setVisibility(View.VISIBLE);
            shadow.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, R.anim.shadow_anim));
        }, 1500);

        fromShare = false;
        mainIntent = getIntent();
        action = mainIntent.getAction();
        Uri data;

        Log.d(TAG, "onCreate: -");

        if (mainIntent.getBooleanExtra("fromNotification", false)) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(mainIntent.getExtras().getInt("id"));
        } else if (action != null) {
            if (action.equals(Intent.ACTION_VIEW)) {
                fromShare = true;
                data = mainIntent.getData();
                action = data.toString();
                //latLng = new LatLng(Double.parseDouble(action.substring(action.indexOf(":")+1,action.indexOf(","))),Double.parseDouble(action.substring(action.indexOf(",")+1,action.indexOf("?"))));
                Log.d(TAG, "onCreate:------+ ");
            }
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(2500);
                } catch (InterruptedException e) {
                    Log.d(TAG, "run: " + e);
                } finally {
                    if (!isServiceRunning(getPackageName() + "." + "LocationUpdatesService")) {
                        goForward();
                    } else {
                        intent = new Intent(SplashActivity.this, MapsActivityFinal.class);
                        intent.putExtra("from", 1);
                        startActivity(intent);
                        finish();
                    }
                    Log.d(TAG, "run: " + intent);

                }
            }
        };
        thread.start();

    }

    private void goForward() {
        Log.d(TAG, "goForward");
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        FirebaseUser user = mAuth.getCurrentUser();
        SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
//        if (user == null) {
        if (false) {
            Log.d(TAG, "goForward: LoginScreen");
            intent = new Intent(SplashActivity.this, LoginActivity.class);
            startAct(intent);
        } else {
            intent = new Intent(SplashActivity.this, MapsActivityPrimary.class);
            startAct(intent);
            /*String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                intent = new Intent(SplashActivity.this, MapsActivityPrimary.class);
                                startAct(intent);
                            } else {
                                Log.d(TAG, "No such document");

                                if (user.getEmail() != null && !user.getEmail().equals("")) {
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("name", user.getDisplayName());
                                    if (user.getEmail() == null) {
                                        userData.put("mail", "");
                                    } else {
                                        userData.put("mail", user.getEmail());
                                        userData.put("login_type", "mail");
                                    }
                                    if (user.getPhoneNumber() == null) {
                                        userData.put("phone", "");
                                    } else {
                                        userData.put("phone", user.getPhoneNumber());
                                    }
                                    userData.put("created", FieldValue.serverTimestamp());
                                    userData.put("last_opened", FieldValue.serverTimestamp());
                                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userData)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Log.d(TAG, "goForward: Details Uploaded.");
                                                    intent = new Intent(SplashActivity.this, MapsActivityPrimary.class);
                                                    startAct(intent);
                                                } else {
                                                    Log.d(TAG, "goForward: Details not uploaded: "+task1.getException().getMessage());
                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Hey, " + user.getDisplayName())
                                                            .setMessage("Please check your Internet Connection and try again.")
                                                            .setPositiveButton("Ok", null)
                                                            .show();
                                                }
                                            });
                                } else {
                                    intent = new Intent(SplashActivity.this, NamePrompt.class);
                                    startAct(intent);
                                }
                            }
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("Hey")
                                    .setMessage("Please check your Internet Connection and try again.")
                                    .setPositiveButton("Ok", null)
                                    .show();
                        }
                    });*/
        }
    }

    private void startAct(Intent intent) {
        SharedPreferences targetDetails = getSharedPreferences("targetDetails", MODE_PRIVATE);
        SharedPreferences.Editor editor = targetDetails.edit();
        editor.clear();
        editor.apply();
        if (mainIntent.getBooleanExtra("fromNotification", false) && !fromShare) {
            SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
            String text = mainIntent.getStringExtra("name");
            Log.d(TAG, "onCreate: " + text + " , " + mainIntent.getExtras());
            double[] latLng = mainIntent.getExtras().getDoubleArray("latlng");
            intent.putExtra("from", "SAN");
            intent.putExtra("name", text);
            intent.putExtra("placeId", mainIntent.getStringExtra("placeId"));
            intent.putExtra("latlng", latLng);
        } else {
            intent.putExtra("from", "SA");
            if (fromShare) {
                intent.putExtra("shared location", action);
                intent.putExtra("from", "shared");
            }
        }

        startActivity(intent);
        finish();
    }

    private boolean isServiceRunning(String serviceName) {
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : l) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                serviceRunning = runningServiceInfo.foreground;
            }
        }
        return serviceRunning;
    }
}
