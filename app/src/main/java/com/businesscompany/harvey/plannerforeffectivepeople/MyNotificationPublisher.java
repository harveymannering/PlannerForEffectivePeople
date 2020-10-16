package com.businesscompany.harvey.plannerforeffectivepeople;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.legacy.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Date;

public class MyNotificationPublisher extends WakefulBroadcastReceiver {

    static int id = 0;
    public static String NOTIFICATION_ID = "notification_id";
    public static String NOTIFICATION = "notification";
    public static String REPEAT_NUMBER = "repeat_number";
    public static String CANCEL_NOTIFICATION = "cancel_notification";
    public static final String ANDROID_CHANNEL_ID = "com.businesscompany.harvey.plannerforeffectivepeople";
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";
    public static NotificationManager notificationManager;

    @Override
    public void onReceive(final Context context, Intent intent) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        //Notification may need to not be publish
        boolean cancel = intent.getBooleanExtra(CANCEL_NOTIFICATION, false);
        if (cancel){
            return;
        }



        if (Build.VERSION.SDK_INT > 26){
            //Create android channel
            NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                    ANDROID_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            androidChannel.enableLights(true);
            //Sets whether notification posted to this channel should vibrate.
            androidChannel.enableVibration(true);
            //Sets whether notifications posted to this channel appear on the lockscreen or not
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(androidChannel);

        }
        else if (Build.VERSION.SDK_INT > 20){
            //Make small icon invisible
            int smallIconId = context.getResources().getIdentifier("right_icon", "id", android.R.class.getPackage().getName());
            if (notification.contentView != null)
                notification.contentView.setViewVisibility(smallIconId, View.INVISIBLE);
            if (notification.headsUpContentView != null)
                notification.headsUpContentView.setViewVisibility(smallIconId, View.INVISIBLE);
            if (notification.bigContentView != null)
                notification.bigContentView.setViewVisibility(smallIconId, View.INVISIBLE);
        }
        else {
            //Make small icon invisible
            int smallIconId = context.getResources().getIdentifier("right_icon", "id", android.R.class.getPackage().getName());
            if (notification.contentView != null)
                notification.contentView.setViewVisibility(smallIconId, View.INVISIBLE);
            if (notification.bigContentView != null)
                notification.bigContentView.setViewVisibility(smallIconId, View.INVISIBLE);
        }

        // Get item id/ notification id
        int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
        int repeats = intent.getIntExtra(REPEAT_NUMBER, 0);
        Log.i("id: ", notificationId + "");

        //Check if the item still exists
        PlannerDatabaseHelper plannerDatabaseHelper = new PlannerDatabaseHelper(context);
        PlannerItem plannerItem = new PlannerItem(0, "", "", 0, new Date(), new Date(), "", 0, "NEVER", 1, 0, 0, 0);
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            if (notificationId < 0) {
                if (plannerDatabaseHelper.repetitionExists(db, (notificationId + repeats) / -10000, repeats))
                    plannerItem = plannerDatabaseHelper.getItem(db, (notificationId + repeats) / -10000);
            }
            else {
                plannerItem = plannerDatabaseHelper.getItem(db, notificationId);
            }
            db.close();
        } catch (Exception e) {
            Toast toast = Toast.makeText(context, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //If id = 0  it means that the item no longer exists in the database
        if (plannerItem.getId() != 0) {
            notificationManager.notify(notificationId, notification);
            try {
                SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
                plannerDatabaseHelper.deleteActiveNotification(db, notificationId);
                db.close();
            } catch (Exception e) {
                Toast toast = Toast.makeText(context, "Database unavailable", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

    }
}
