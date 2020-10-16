package com.businesscompany.harvey.plannerforeffectivepeople;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    PlannerDatabaseHelper plannerDatabaseHelper;
    static int pos = -1;
    int width;
    int tableRows = 0;
    public static final String ANDROID_CHANNEL_ID = "com.businesscompany.harvey.plannerforeffectivepeople";
    public static Date selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Will hide the title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        //Define datebase
        plannerDatabaseHelper = new PlannerDatabaseHelper(this);
    }

    protected void onStart()
    {
        super.onStart();
        //Set up the title bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setTitle("");
    }

    protected void onResume(){

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        ParentPlannerFragment fragment = new ParentPlannerFragment();
        fragmentTransaction.add(R.id.container, fragment);
        fragmentTransaction.commit();

        super.onResume();
        //Check for notifications
        try {
            //values from the database
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            boolean notificationEnabled = plannerDatabaseHelper.isSettingDisplayed(db, "NOTIFICATIONS_ENABLED");
            int minutesDelay = plannerDatabaseHelper.getNotificationMinutes(db);
            ArrayList<Integer> expectedNotificationIds = plannerDatabaseHelper.getActiveNotifications(db);
            ArrayList<Integer> actualNotificationIds = new ArrayList<Integer>();

            if (notificationEnabled) {



                //Adding a extra three months to the end day (just to make sure no items, including repeats, are missed)
                Calendar c1 = Calendar.getInstance();
                c1.setTime(plannerDatabaseHelper.getLastDate(db));
                Calendar c2 = Calendar.getInstance();
                c2.add(Calendar.MONTH, 3);
                if (c1.getTime().before(c2.getTime()))
                    c1.setTime(c2.getTime());


                List<PlannerItem> items = plannerDatabaseHelper.getItems(
                        db,
                        0,
                        Calendar.getInstance().getTime(),
                        c1.getTime()
                );
                long minutesMilis = minutesDelay * 60 * 1000; //x minutes
                for (PlannerItem item : items){
                    Calendar eventTime = Calendar.getInstance();
                    c1.setTime(item.getStartDate());
                    SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                    long millisUntilEvent = (c1.getTimeInMillis()-System.currentTimeMillis());
                    if ((millisUntilEvent - minutesMilis) >= 0) {
                        int repeats = item.getEnds_after();
                        int notificationId = item.getId();
                        if (item.getRepeat_interval().equals("NEVER"))
                            repeats = 0;
                        else
                            notificationId = -(notificationId * 10000) - repeats;

                        scheduleNotification(getNotification(sdf1.format(item.getStartDate()) + " - " + item.getTitle(), item.getDescription(), item.getId(), sdf2.format(item.getStartDate()), repeats),
                                (millisUntilEvent - minutesMilis),
                                item.getId(),
                                repeats);

                        actualNotificationIds.add(notificationId);
                    }
                }

                // Are there any notification that should have been publish that haven't?
                // if yes they should be cancelled (its possible that if a repeating task has its date changed, residue notification can be shown to the user at the incorrect time)
                for (Integer expectedId : expectedNotificationIds){
                    if (!actualNotificationIds.contains(expectedId)){
                        cancelNotification(getNotification("00:00 - Error: This item no longer exists", "", 0, "2020/01/01", 0),
                                0,
                                expectedId);
                        plannerDatabaseHelper.deleteActiveNotification(db, expectedId);
                    }
                }

                // Which notification have been publish which had not been previously been published?
                // The ones that have should be added to the Active_Notifications table to be kept track of
                ArrayList<Integer> newNotificationIds = new ArrayList<Integer>();
                for (Integer actualId : actualNotificationIds){
                    if (!expectedNotificationIds.contains(actualId)){
                        newNotificationIds.add(actualId);
                    }
                }
                plannerDatabaseHelper.addActiveNotifications(db, newNotificationIds);
            }
            else {
                for (Integer expectedId : expectedNotificationIds) {
                    cancelNotification(getNotification("00:00 - Error: This item no longer exists", "", 0, "2020/01/01", 0),
                            0,
                            expectedId);
                }
            }
        }
        catch (Exception e) {

        }
    }

    @Override
    public void onRestart(){
        finish();
        startActivity(getIntent());
        super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_menu: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
            // case blocks for other MenuItems (if any)
        }
        return true;
    }

    private void scheduleNotification(Notification notification, long delay, int notificationId, int repeats) {

        Intent notificationIntent = new Intent(this, MyNotificationPublisher.class);

        //If the it is a repeating task
        if (repeats != 0)
            //COULD CAUSE ISSUES: this'll mean that after 210000 items or 100000 repeats of the same item, notifications will not work
            notificationId = -(notificationId * 10000) - repeats;

        //Add parameters to the notification
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtra(MyNotificationPublisher.REPEAT_NUMBER, repeats);
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION, notification);
        notificationIntent.putExtra(MyNotificationPublisher.CANCEL_NOTIFICATION, false);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Time until notifiation should be publish (in miliseconds)
        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        //Schedule the nofication
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private void cancelNotification(Notification notification, long delay, int notificationId){
        Intent notificationIntent = new Intent(this, MyNotificationPublisher.class);

        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtra(MyNotificationPublisher.REPEAT_NUMBER, 0);
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION, notification);
        notificationIntent.putExtra(MyNotificationPublisher.CANCEL_NOTIFICATION, true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }


    private Notification getNotification(String title, String content, int id, String date, int repeats) {
        //Resources
        Drawable drawable = ContextCompat.getDrawable(this,R.mipmap.ic_launcher);
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, ViewItemActivity.class);
        //Bundle that will be passed to the next activity
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        bundle.putString("date", date);
        bundle.putInt("repeats", repeats);
        resultIntent.putExtras(bundle);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        if (repeats != 0)
            id = -(id * 10000) - repeats; //repurpose id variable
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);

        //Build notifications
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT > 26)
            builder = new Notification.Builder(this, ANDROID_CHANNEL_ID);
        else
            builder = new Notification.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setContentIntent(resultPendingIntent);
        builder.setSmallIcon(R.drawable.ic_event);
        builder.setLargeIcon(bitmap);
        builder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        builder.setAutoCancel(true); //Allows users to remove notifications
        return builder.build();
    }

    protected void onStop() {
        super.onStop();

        try {
            pos = ParentPlannerFragment.mViewPager.getCurrentItem();
        }
        catch (NullPointerException npe){
            pos = 0;
        }

        for (int i = 0; i < ParentPlannerFragment.d.size(); i++) {
            if (i == pos){
                selectedTime = ParentPlannerFragment.d.get(i);
                break;
            }
        }
    }

}