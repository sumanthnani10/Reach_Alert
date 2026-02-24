package com.ismartapps.reachalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME =
            "com.ismartapps.reachalert";
    private static final String TAG = LocationUpdatesService.class.getSimpleName();
    private static final String CHANNEL_ID = "channel_01";
    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 20000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;
    private Bitmap targetImage;
    private LatLng target,current;
    private double radius;
    private Location mLocation;
    public boolean isCancelled=false;
    private static String name, trackId;
    private SharedPreferences targetDetails;

    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        targetDetails = getSharedPreferences("targetDetails",MODE_PRIVATE);
        target = new LatLng((double) targetDetails.getFloat("targetLat",0),(double) targetDetails.getFloat("targetLang",0));
        targetImage = null;
        String placeId = targetDetails.getString("targetId", null);
        if(placeId !=null)
        {
            this.setTargetImage(placeId);
        }

        name = targetDetails.getString("targetName","Destination");
        trackId = targetDetails.getString("trackID","Destination");
        Log.d(TAG, "onCreate: "+trackId);
        radius = (double) targetDetails.getFloat("targetRad",500);

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setShowBadge(true);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    public void setTargetImage(String placeId) {

        PlacesClient placesClient = Places.createClient(this);
        List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS,Place.Field.NAME);
        FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, fields).build();
        placesClient.fetchPlace(placeRequest).addOnSuccessListener((response) -> {
            Place place = response.getPlace();

            if (place.getPhotoMetadatas() != null) {
                if (place.getPhotoMetadatas().size() > 0) {
                    // Get the photo metadata.
                    PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);

                    // Get the attribution text.
                    String attributions = photoMetadata.getAttributions();
                    // Create a FetchPhotoRequest.
                    FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                            .build();
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener(fetchPhotoResponse -> {
                        targetImage = fetchPhotoResponse.getBitmap();
                    }).addOnFailureListener((exception) -> {
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            int statusCode = apiException.getStatusCode();
                            Log.e(TAG, "Place not found: (Primary) " + exception.getMessage()+" , "+statusCode);
                        }
                    });
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started"+flags);
        String action = intent.getAction();

        if(action!=null)
        if(action.equals(ACTION_STOP_FOREGROUND_SERVICE))
        {
            isCancelled=true;
            SharedPreferences.Editor editor = targetDetails.edit();
            editor.clear();
            editor.apply();
            removeLocationUpdates();
            try{
                Log.d(TAG, "onStartCommand: CANCELLED");
                Intent intent1 = new Intent(ACTION_BROADCAST);
                intent1.putExtra("Stop", true);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent1);
            }
            catch (Exception e)
            {
                Log.d(TAG, "onStartCommand: "+e);
            }
            finally {
                if (intent.getBooleanExtra("update_cancel",true)) {
                    /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update("tracks." + trackId + ".cancelled", FieldValue.serverTimestamp())
                            .addOnCompleteListener(t -> {
                                if (t.isSuccessful()) {
                                    Log.d(TAG, "updated cancelled successfully");
                                } else {
                                    Log.d(TAG, "updated cancelled not successful");
                                }
                                stopForeground(true);

                            });*/
                    stopSelf();
                } else {
                    stopForeground(true);
                    stopSelf();
                }
            }
        }
        else 
        {
            requestLocationUpdates();
            startForeground(NOTIFICATION_ID, getNotification(current));
            Log.d(TAG, "onStartCommand: ");
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            Log.i(TAG, "Requesting location updates");

        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    private Notification getNotification(LatLng latLng) {
        Intent intent = new Intent(this, LocationUpdatesService.class);
        intent.setAction(LocationUpdatesService.ACTION_STOP_FOREGROUND_SERVICE);

        Intent intent1 = new Intent(this,SplashActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this,0,intent1,PendingIntent.FLAG_IMMUTABLE);

        CharSequence text = getDistance(name,latLng);
        Log.d(TAG, "getNotification: "+new Date() + text);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        //on Cancel Click
        PendingIntent cancelPendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setColor(Color.RED)
                .setSmallIcon(R.drawable.notification_small_icon)
                .setContentTitle("You're All Set")
                .setContentText(text)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.notification_small_icon,"Cancel",cancelPendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = settings.getBoolean("dark",false);
        if(dark)
        {
            builder.setColor(Color.BLACK)
                    .setColorized(true);
        }

        if (targetImage!=null)
        {
            builder.setLargeIcon(targetImage)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(targetImage)
                            .bigLargeIcon(targetImage));
        }

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    private void getLastLocation() {
        Log.d(TAG, "getLastLocation");
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                                setCurrent(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()));
                                Log.d(TAG, "onComplete: "+mLocation.toString());
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        //Intent intent1 = new Intent(ACTION_STOP_FOREGROUND_SERVICE);
        intent.putExtra(EXTRA_LOCATION, location);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent1);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        setCurrent(new LatLng(location.getLatitude(),location.getLongitude()));

        mNotificationManager.notify(NOTIFICATION_ID, getNotification(new LatLng(location.getLatitude(),location.getLongitude())));
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private String getDistance(String name, LatLng latLng)
    {
        float[] results = new float[1];
        if (current!=null){
        Location.distanceBetween(target.latitude,target.longitude,latLng.latitude,latLng.longitude,results);
        String dist=name+" is ";
        if(results[0]-radius<=0)
        {
            return name+" is Reached.";
        }
        else if (results[0]-radius>1000){
            dist= dist + String.format("%.2f",(results[0]-radius)/1000)+" km away";
        }
        else dist =  dist + ((int) results[0] - radius) +" m away";

        return dist;
        }
        else
            return "Happy Journey";
    }

    public static String getName()
    {
        return name;
    }

    private void setCurrent(LatLng current) {
        Log.d(TAG, "setCurrent");
        this.current = current;
    }
}