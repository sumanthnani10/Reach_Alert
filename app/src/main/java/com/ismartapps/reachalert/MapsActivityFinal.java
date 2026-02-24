package com.ismartapps.reachalert;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MapsActivityFinal extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;
    private TextView targetPlaceName, targetPlaceType, targetPlaceAddress, cancel, changeRadius;
    private CardView searchCard, radiusControlCard;
    private Circle circle = null;
    private Marker marker = null;
    private ImageView mCurrLoc, zoomIn, zoomOut;
    private CardView mCancel;
    private LatLng targetLatLng;
    private static FusedLocationProviderClient mFusedLocationClient;
    public static double[] tempd = new double[3];
    private TargetDetails targetDetails;
    private DrawerLayout drawerLayout;
    private int activityCount;
    private String userName, placeId;
    private SharedPreferences targetDetail;

    private static final String TAG = MapsActivityFinal.class.getSimpleName();
    private MyReceiver myReceiver = new MyReceiver();
    private Activity activity = this;
    private boolean dark, fromNotification;
    private double radius;
    private ConfirmationDialog confirmationDialog;
    private int back = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        dark = settings.getBoolean("dark", false);
        if (dark)
            setContentView(R.layout.activity_maps_dark);
        else {
            setTheme(R.style.AppTheme);
            setContentView(R.layout.activity_maps);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps_primary);
        mapFragment.getMapAsync(this);
        activityCount = 0;

        targetDetail = getSharedPreferences("targetDetails", MODE_PRIVATE);

        Log.d(TAG, "onCreate: Final");
    }

    @Override
    protected void onResume() {
        super.onResume();
        back = 0;
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        if (activityCount == 0) {
            activityCount++;
        } else {
            showAd();
        }
    }

    private void init() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        Intent intent = getIntent();
        changeRadius.setVisibility(View.VISIBLE);

        if (intent.getIntExtra("from", 2) == 2) {
            targetDetails = intent.getExtras().getParcelable("targetDetails");
            targetPlaceName.setText(targetDetails.getName());
            targetPlaceAddress.setText(targetDetails.getAddress());
            targetLatLng = targetDetails.getTarget();
            radius = targetDetails.getRadius();
            placeId = targetDetails.getPlaceId();

            SharedPreferences.Editor editor = targetDetail.edit();
            editor.putString("targetName", targetPlaceName.getText().toString());
            editor.putString("targetAddress", targetPlaceAddress.getText().toString());
            editor.putFloat("targetLat", (float) targetLatLng.latitude);
            editor.putFloat("targetLang", (float) targetLatLng.longitude);
            editor.putFloat("targetRad", (float) radius);
            editor.putString("targetId", placeId);
            editor.putString("trackID", targetDetails.getTrackID());
            editor.apply();
        } else {
            fromNotification = true;
            targetPlaceName.setText(targetDetail.getString("targetName", "Place Name"));
            targetPlaceAddress.setText(targetDetail.getString("targetAddress", "Address"));
            targetLatLng = new LatLng((double) targetDetail.getFloat("targetLat", 0), (double) targetDetail.getFloat("targetLang", 0));
            radius = (double) targetDetail.getFloat("targetRad", 500);
            placeId = targetDetail.getString("targetId", null);
            showAd();
        }

        targetPlaceName.setGravity(Gravity.END);
        targetPlaceType.setGravity(Gravity.END);

        marker = null;
        cancel.setText("Stop");
        circle = null;
//        if(dark)
//            mCancel.setImageResource(R.mipmap.ic_launcher_cancel_dark);
//        else
//            mCancel.setImageResource(R.mipmap.ic_launcher_cancel);
        mCancel.setVisibility(View.VISIBLE);
        targetPlaceType.setText(getResources().getString(R.string.target_yet_to_reach));
        radiusControlCard.setVisibility(View.GONE);
        mCurrLoc.setVisibility(View.INVISIBLE);
        searchCard.setVisibility(View.INVISIBLE);


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15f));

        marker = mMap.addMarker(new MarkerOptions().position(targetLatLng).title(targetPlaceName.getText().toString()).draggable(false));
        circle = mMap.addCircle(new CircleOptions()
                .center(targetLatLng)
                .strokeWidth(3)
                .radius(radius)
                .strokeColor(R.color.imageColor3));

        circle.setFillColor(0x22ffff00);

        if (dark) {
            circle.setStrokeColor(R.color.quantum_black_100);
//            circle.setFillColor(R.color.white);
        }

        tempd[0] = targetLatLng.latitude;
        tempd[1] = targetLatLng.longitude;
        tempd[2] = circle.getRadius();

        Calendar calendar = Calendar.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);

        /*if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals("") && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
            uploadData(true, FirebaseAuth.getInstance().getCurrentUser().getEmail(), FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                    targetPlaceName.getText().toString(), targetPlaceAddress.getText().toString(), targetLatLng,
                    new SimpleDateFormat("dd-MMM-yyyy,E hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            userName = sharedPreferences.getString("dbname", "User Name");
        } else {
            uploadData(false, FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(), sharedPreferences.getString("name", "User Name"),
                    targetPlaceName.getText().toString(), targetPlaceAddress.getText().toString(), targetLatLng,
                    new SimpleDateFormat("dd-MMM-yyyy,E hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            userName = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        }*/

        if (!fromNotification)
            scheduleNotification();

        getLastLocation();

        mCancel.setOnLongClickListener(view -> {
            Toast.makeText(MapsActivityFinal.this, "Cancel Tracking", Toast.LENGTH_SHORT).show();
            return true;
        });

        cancel.setVisibility(View.VISIBLE);

        ConfirmationDialog confirmationDialogChangeRadius = new ConfirmationDialog(this, "Change Radius",
                "Do you want to change Radius?", activity) {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.confirm_button:

                        Toast.makeText(MapsActivityFinal.this, "Stopped Tracking", Toast.LENGTH_SHORT).show();
//                        updateCancelled();
                        Intent intent1 = new Intent(MapsActivityFinal.this, LocationUpdatesService.class);
                        intent1.setAction(LocationUpdatesService.ACTION_STOP_FOREGROUND_SERVICE);
                        startService(intent1);
                        Intent intent2 = new Intent(MapsActivityFinal.this, MapsActivitySecondary.class);
                        intent2.putExtra("targetDetails", targetDetails);
                        startActivity(intent2);
                        showAd();
                        dismiss();
                        finish();
                        break;

                    case R.id.no_button:
                        dismiss();
                        break;
                }
            }
        };
        confirmationDialogChangeRadius.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        changeRadius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmationDialogChangeRadius.show();
            }
        });

        int i = 0;
        confirmationDialog = new ConfirmationDialog(this, "Cancel Tracking",
                "Do you want to cancel tracking?", activity) {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.confirm_button:

                        Toast.makeText(MapsActivityFinal.this, "Stopped Tracking", Toast.LENGTH_SHORT).show();
                        showLoading(true);
                        Intent intent1 = new Intent(MapsActivityFinal.this, LocationUpdatesService.class);
                        intent1.setAction(LocationUpdatesService.ACTION_STOP_FOREGROUND_SERVICE);
                        startService(intent1);
                        if (fromNotification) {
                            startActivity(new Intent(MapsActivityFinal.this, MapsActivityPrimary.class));
                        }
                        showAd();
                        dismiss();
                        finish();
                        break;

                    case R.id.no_button:
                        dismiss();
                        break;
                }
            }
        };
        confirmationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mCancel.setOnClickListener(view -> {
            confirmationDialog.show();
        });

        cancel.setOnClickListener(view ->
        {
            confirmationDialog.show();
        });
    }

    private void stopTracking() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("Stop", false)) {
                stopTracking();
//                updateCancelled();
                try {
                    if (fromNotification) {
                        startActivity(new Intent(MapsActivityFinal.this, MapsActivityPrimary.class));
                    }
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
                if (location != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(targetLatLng.latitude, targetLatLng.longitude, location.getLatitude(), location.getLongitude(), results);
                    try {
                        moveCamera(targetLatLng, new LatLng(location.getLatitude(), location.getLongitude()));
                    } catch (Exception e) {
                        Log.d(TAG, "onReceive: " + e);
                    }
                    if (results[0] <= radius) {
                        Intent intent1 = new Intent(context, LocationUpdatesService.class);
                        intent1.setAction(LocationUpdatesService.ACTION_STOP_FOREGROUND_SERVICE);
                        intent1.putExtra("update_cancel",false);
                        startService(intent1);
                        Utils.sendNotificationOnComplete(LocationUpdatesService.getName(), context);
                        Utils.playRing(context);
                        stopTracking();

                        /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update("tracks."+targetDetails.getTrackID()+".reached", FieldValue.serverTimestamp())
                                .addOnCompleteListener(t -> {
                                    if(t.isSuccessful()){
                                        Log.d(TAG, "updated reached successfully");
                                    } else {
                                        Log.d(TAG, "updated reached not successful");
                                    }
                                });*/
                        try {
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void initVars() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        targetPlaceName = findViewById(R.id.place_name);
        targetPlaceType = findViewById(R.id.place_type);
        targetPlaceAddress = findViewById(R.id.place_address);
        cancel = findViewById(R.id.cancel);
        mCurrLoc = findViewById(R.id.location_btn_img);
        searchCard = findViewById(R.id.searchbar_layout_card);
        mCancel = findViewById(R.id.place_cancel_image);
        radiusControlCard = findViewById(R.id.radius_controller_container_card);
        zoomIn = findViewById(R.id.zoom_in);
        zoomIn.setVisibility(View.INVISIBLE);
        zoomOut = findViewById(R.id.zoom_ot);
        zoomOut.setVisibility(View.INVISIBLE);
        drawerLayout = findViewById(R.id.drawer_layout);
        changeRadius = findViewById(R.id.change_radius);
    }

    /*private void uploadData(boolean mail, String email, String name, String targetName, String targetAddress, LatLng targetlatLng, String time) {
        UserLatestData user = new UserLatestData(targetName, targetAddress, targetlatLng, time);
        if (mail) {
            databaseReference.child("Last Used").child(name).setValue(user);
            if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null)
                databaseReference.child("Last Used").child(name).child("Phone").setValue(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
            databaseReference.child("Last Used").child(name).child("Status").child("Reached").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(name).child("Status").child("Cancelled").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(name).child("Status").child("Running").setValue("true " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(name).child("Status").child("Stopped Ring").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
        } else {
            databaseReference.child("Last Used").child(email).setValue(user);
            if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null)
                databaseReference.child("Last Used").child(email).child("Phone").setValue(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
            databaseReference.child("Last Used").child(email).child("Status").child("Reached").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(email).child("Status").child("Cancelled").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(email).child("Status").child("Running").setValue("true " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));
            databaseReference.child("Last Used").child(email).child("Status").child("Stopped Ring").setValue("false " + new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz", new Locale("EN")).format(new Date()));

        }
    }*/

    private void updateRecents(String locationName, LatLng latLng, String placeId) {
        SharedPreferences recents = getSharedPreferences("recent", MODE_PRIVATE);
        SharedPreferences.Editor editor = recents.edit();
        Log.d(TAG, "updateRecents: " + locationName + latLng + placeId);

        if (((placeId == null || placeId.equals("")) && (!(latLng.latitude == recents.getFloat("recent_one_lat", 0) && latLng.longitude == recents.getFloat("recent_one_long", 0))
                && !(latLng.latitude == recents.getFloat("recent_two_lat", 0) && latLng.longitude == recents.getFloat("recent_two_long", 0))
                && !(latLng.latitude == recents.getFloat("recent_three_lat", 0) && latLng.longitude == recents.getFloat("recent_three_long", 0))))
                || (placeId != null && (!placeId.equals(recents.getString("recent_one_pid", null)) && !placeId.equals(recents.getString("recent_two_pid", null))
                && !placeId.equals(recents.getString("recent_three_pid", null))))
        ) {
            Log.d(TAG, "updateRecents: 1:" + latLng.latitude + " : " + recents.getFloat("recent_one_lat", 0) + "2:" + latLng.longitude + " : " + recents.getFloat("recent_one_long", 0));
            editor.putString("recent_three", recents.getString("recent_two", "Recent Location"));
            editor.putFloat("recent_three_lat", recents.getFloat("recent_two_lat", 0));
            editor.putFloat("recent_three_long", recents.getFloat("recent_two_long", 0));
            editor.putString("recent_three_pid", recents.getString("recent_two_pid", ""));
            editor.putString("recent_two", recents.getString("recent_one", "Recent Location"));
            editor.putFloat("recent_two_lat", recents.getFloat("recent_one_lat", 0));
            editor.putFloat("recent_two_long", recents.getFloat("recent_one_long", 0));
            editor.putString("recent_two_pid", recents.getString("recent_one_pid", ""));
            editor.putString("recent_one", locationName);
            editor.putFloat("recent_one_lat", (float) latLng.latitude);
            editor.putFloat("recent_one_long", (float) latLng.longitude);
            editor.putString("recent_one_pid", placeId);
            editor.apply();
        } else if (latLng.latitude == recents.getFloat("recent_two_lat", 0) && latLng.longitude == recents.getFloat("recent_two_long", 0)) {
            Log.d(TAG, "updateRecents: 2");
            editor.putString("recent_two", recents.getString("recent_one", "Recent Location"));
            editor.putFloat("recent_two_lat", recents.getFloat("recent_one_lat", 0));
            editor.putFloat("recent_two_long", recents.getFloat("recent_one_long", 0));
            editor.putString("recent_two_pid", recents.getString("recent_one_pid", ""));
            editor.putString("recent_one", locationName);
            editor.putFloat("recent_one_lat", (float) latLng.latitude);
            editor.putFloat("recent_one_long", (float) latLng.longitude);
            editor.putString("recent_one_pid", placeId);
            editor.apply();
        }
    }

    void scheduleNotification() {
        Calendar calendar = Calendar.getInstance();
        PendingIntent notificationPendingIntent;
        Intent notificationIntent = new Intent(this, RemainderReceiver.class);
        notificationIntent.putExtra("text", targetPlaceName.getText().toString());
        notificationIntent.putExtra("placeId", targetDetails.getPlaceId());
        notificationIntent.putExtra("latlng", new double[]{targetLatLng.latitude, targetLatLng.longitude});

        if (targetDetails.getPlaceId() != null)
            updateRecents(targetPlaceName.getText().toString(), new LatLng(targetLatLng.latitude, targetLatLng.longitude), targetDetails.getPlaceId());
        else
            updateRecents(targetPlaceName.getText().toString(), new LatLng(targetLatLng.latitude, targetLatLng.longitude), "");

        Log.d(TAG, "scheduleNotification: " + targetPlaceName.getText().toString() + " ," + targetDetails.getPlaceId() + " ," + targetDetails.getPlaceId());


        SharedPreferences notification = getSharedPreferences("notifications", MODE_PRIVATE);
        SharedPreferences.Editor editor = notification.edit();

        if (calendar.get(Calendar.HOUR_OF_DAY) >= 0 && calendar.get(Calendar.HOUR_OF_DAY) < 12) {
            editor.putBoolean("morning", true);
            notificationIntent.putExtra("id", 3);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 3, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else if (calendar.get(Calendar.HOUR_OF_DAY) >= 12 && calendar.get(Calendar.HOUR_OF_DAY) < 17) {
            editor.putBoolean("afternoon", true);
            notificationIntent.putExtra("id", 4);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 4, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            editor.putBoolean("evening", true);
            notificationIntent.putExtra("id", 5);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 5, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        }

        editor.apply();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, notificationPendingIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (back == 0) {
            back++;
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    back = 0;
                }
            }, 1000);
            Toast.makeText(this, "Press Back again to exit", Toast.LENGTH_SHORT).show();
        } else {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
    }

    public static void moveCamera(LatLng target, LatLng current) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(target);
        builder.include(current);
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        CameraPosition cameraPosition = mMap.getCameraPosition();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                .target(cameraPosition.target)
                .tilt(67.5f)
                .zoom(cameraPosition.zoom)
                .build()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: (Final) map is ready");
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setPadding(0, 0, 0, 400);
        initVars();
        init();
        if(dark)
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.mapstyle_night));
    }

    private void getLastLocation() {
        Log.d(TAG, "getLastLocation");
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location mLocation = task.getResult();
                                moveCamera(targetLatLng,new LatLng(mLocation.getLatitude(),mLocation.getLongitude()));
                                if (!fromNotification)
                                    startTracking();
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            if (!fromNotification)
                startTracking();
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void startTracking() {
        Intent intent = new Intent(this,LocationUpdatesService.class);
        intent.setAction(LocationUpdatesService.EXTRA_LOCATION);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void showAd()
    {}

    private void showLoading(boolean show) {
        RelativeLayout loading = findViewById(R.id.loading);
        if(show) {
            loading.setVisibility(View.VISIBLE);
        } else {
            loading.setVisibility(View.GONE);
        }
    }

    /*private void updateReached()
    {
        SharedPreferences recents = getSharedPreferences("recent", MODE_PRIVATE);
        SharedPreferences.Editor editor = recents.edit();
        editor.putBoolean("Reached Location?",true);
        editor.apply();
        databaseReference.child("Last Used").child(userName).child("Status").child("Reached").setValue("true "+new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz",new Locale("EN")).format(new Date()));
        databaseReference.child("Last Used").child(userName).child("Status").child("Running").setValue("false "+new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz",new Locale("EN")).format(new Date()));
    }

    private void updateCancelled()
    {
        databaseReference.child("Last Used").child(userName).child("Status").child("Reached").setValue("true "+new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz",new Locale("EN")).format(new Date()));
        databaseReference.child("Last Used").child(userName).child("Status").child("Cancelled").setValue("true "+new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz",new Locale("EN")).format(new Date()));
        databaseReference.child("Last Used").child(userName).child("Status").child("Running").setValue("false "+new SimpleDateFormat("dd-MMM-yy hh:mm:ss a zzzz",new Locale("EN")).format(new Date()));
    }*/
}