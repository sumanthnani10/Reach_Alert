package com.ismartapps.reachalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


class Utils {
    private static String TAG="Utils";

    private final static String KEY_LOCATION_UPDATES_REQUESTED = "location-updates-requested";
    private static NotificationManager mNotificationManager;
    private static NotificationCompat.Builder builder;

    static void setRequestingLocationUpdates(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value)
                .apply();
    }

    static void sendNotificationOnComplete(String placeName, Context context) {
        //on Stop Click
        Intent stopRingIntent = new Intent(context,StopRing.class);
        PendingIntent stopRingPendingIntent = PendingIntent.getBroadcast(context,0,stopRingIntent,PendingIntent.FLAG_IMMUTABLE);

        Intent fullscreenIntent = new Intent(Intent.ACTION_MAIN,null);
        fullscreenIntent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        fullscreenIntent.setClass(context,FullScreenIntent.class);
        fullscreenIntent.putExtra("location_name",placeName);
        PendingIntent fullscreenPendingIntent = PendingIntent.getActivity(context,22,fullscreenIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        placeName ="Reached "+placeName;

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        // Get a notification builder that's compatible with platform versions >= 4
        builder = new NotificationCompat.Builder(context,"Reached")
                .setSmallIcon(R.drawable.notification_small_icon)
                .setColor(Color.GREEN)
                .setContentTitle("Reached")
                .setContentText(placeName)
                .setContentIntent(fullscreenPendingIntent)
                .setFullScreenIntent(fullscreenPendingIntent,true)
                .addAction(R.drawable.notification_small_icon,"Stop",stopRingPendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(true);

        SharedPreferences settings = context.getSharedPreferences("settings", 0);
        boolean dark = settings.getBoolean("dark",false);
        if(dark)
        {
            builder.setColor(Color.BLACK);
            builder.setColorized(true);
        }

        // Get an instance of the Notification manager
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel("Reached", name, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setShowBadge(true);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mChannel.setLightColor(Color.GREEN);
            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);

            // Channel ID
            builder.setChannelId("Reached");
        }

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    private static Ringtone r;
    private static Timer timer;
    private static Vibrator vibrator;

    static void playRing(Context context){
        Log.d(TAG, "playRing");
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(context,notification);
        r.setStreamType(AudioManager.STREAM_ALARM);

        if(!r.isPlaying()) r.play();

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0,1000,1000};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern,0));
        }
        else
            vibrator.vibrate(pattern,0);


        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!r.isPlaying()) r.play();
                Log.d(TAG, "run ");
            }
        },1000,1000);

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            stopRing(context);
            clearNotifications();
            timer.cancel();
        }, 5*60*1000);
    }

    static void stopRing(Context context){
        Log.d(TAG, "Stopping Ring");
        if (r.isPlaying())
        {r.stop();}

        if(vibrator!=null)
        {
            vibrator.cancel();
        }

        timer.cancel();
    }

    static void clearNotifications(){
        Log.d(TAG, "clearNotifications");
        mNotificationManager.cancelAll();
    }

    static String generateId(String pref) {
        String chars = "zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA9876543210zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA9876543210";
        int char_length = chars.length();
        Date t = new Date();
//        LocalDate localDate = t.toInstant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        calendar.setTime(t);
        String id = pref;
        // id += chars[(t.year / 100).round()];
        id += chars.charAt(calendar.get(Calendar.YEAR) % char_length);
        id += chars.charAt(calendar.get(Calendar.MONTH)+1);
        id += chars.charAt(calendar.get(Calendar.DATE));
        id += chars.charAt(calendar.get(Calendar.HOUR_OF_DAY));
        id += chars.charAt(calendar.get(Calendar.MINUTE));
        id += chars.charAt(calendar.get(Calendar.SECOND));
        id += chars.charAt(calendar.get(Calendar.MILLISECOND) % char_length);
//        id += chars.charAt(calendar.get(Calendar.MILLISECOND) % char_length);
        return id;
    }

}