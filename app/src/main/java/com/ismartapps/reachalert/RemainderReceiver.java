package com.ismartapps.reachalert;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class RemainderReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Log.d("RR", "onReceive"+intent.getStringExtra("text"));

        if(!isServiceRunning("com.ismartapps.reachalert.LocationUpdatesService")) {

            NotificationManager mNotificationManager;

            String text = intent.getStringExtra("text");
            int id = intent.getExtras().getInt("id");

            Log.d("RR", "onReceive: "+id);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Remainder");
            String content;

            double[] latLng = intent.getExtras().getDoubleArray("latlng");
            Intent notificationIntent;
            notificationIntent = new Intent(context, SplashActivity.class);
            if (intent.getBooleanExtra("New?", false)) {
                content = "Just Set And Sleep";
            } else {
                notificationIntent.putExtra("fromNotification", true);
                notificationIntent.putExtra("name", text);
                notificationIntent.putExtra("placeId", intent.getStringExtra("placeId"));
                notificationIntent.putExtra("latlng", latLng);
                notificationIntent.putExtra("id", id);
                Log.d("RR", "onReceive: " + text + " ," + id + " ," + intent.getStringExtra("placeId") + " ," + latLng.toString());
                content = "You visited this place.\nPlanning to go again?";
            }
            PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, 123, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);


            builder.setSmallIcon(R.drawable.notification_small_icon)
                    .setColor(Color.YELLOW)
                    .setContentTitle(text)
                    .setSmallIcon(R.drawable.notification_small_icon)
                    .setContentText(content)
                    .setContentIntent(notificationPendingIntent)
                    .addAction(R.drawable.notification_small_icon, "Yes", notificationPendingIntent)
                    .setTicker(text + content);

            mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = context.getString(R.string.app_name);
                NotificationChannel mChannel =
                        new NotificationChannel("Remainder", name, NotificationManager.IMPORTANCE_HIGH);
                mChannel.setShowBadge(true);
                mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mChannel.setLightColor(Color.YELLOW);

                mNotificationManager.createNotificationChannel(mChannel);

                builder.setChannelId("Remainder");
            }
            mNotificationManager.notify(id, builder.build());


            /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update("last_notified", FieldValue.serverTimestamp())
                    .addOnCompleteListener(t -> {
                    });*/
        }
    }

    private boolean isServiceRunning(String serviceName){
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : l) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)) {
                serviceRunning = runningServiceInfo.foreground;
            }
        }
        return serviceRunning;
    }
}
