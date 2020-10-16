package com.businesscompany.harvey.plannerforeffectivepeople;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    //Database
    PlannerDatabaseHelper plannerDatabaseHelper;
    int startDay = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_setting);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(0xFFFFFFFF);

        //Get database info
        plannerDatabaseHelper = new PlannerDatabaseHelper(this);
        SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
        boolean notificationEnabled = plannerDatabaseHelper.isSettingDisplayed(db, "NOTIFICATIONS_ENABLED");
        boolean displayDatesEnabled = plannerDatabaseHelper.isSettingDisplayed(db, "DISPLAY_DATES");
        startDay = plannerDatabaseHelper.getStartDay(db);
        int notificationMinutes = plannerDatabaseHelper.getNotificationMinutes(db);
        db.close();

        //Initalize the views on the page
        Switch notificationSwitch = (Switch) findViewById(R.id.notification_switch);
        Switch datesSwitch = (Switch) findViewById(R.id.individual_dates_switch);
        final EditText inputNotification = (EditText) findViewById(R.id.input_notification_minutes);
        /*inputNotification.addTextChangedListener(new TextWatcher() {
            boolean addedSuffix = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // if the only text is the suffix
                if(s.toString().equals(SUFFIX)){
                    inputNotification.setText(""); // clear the text
                    return;
                }

                // If there is text append on SUFFIX as long as it is not there
                // move cursor back before the suffix
                if(s.length() > 0 && !s.toString().contains(SUFFIX) && !s.toString().equals(SUFFIX)){
                    String text = s.toString().concat(SUFFIX);
                    inputNotification.setText(text);
                    inputNotification.setSelection(text.length() - SUFFIX.length());
                    addedSuffix = true; // flip the addedSuffix flag to true
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0){
                    addedSuffix = false; // reset the addedSuffix flag
                }
            }
        });*/

        inputNotification.setText(notificationMinutes + "");


        //Hide/show logic for the notification minutes edit text
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView txtNotification = (TextView) findViewById(R.id.txt_notification_minutes);
                EditText inputNotification = (EditText) findViewById(R.id.input_notification_minutes);
                if (!isChecked){
                    txtNotification.setVisibility(View.GONE);
                    inputNotification.setVisibility(View.GONE);
                }
                else {
                    txtNotification.setVisibility(View.VISIBLE);
                    inputNotification.setVisibility(View.VISIBLE);
                }

            }
        });
       notificationSwitch.setChecked(notificationEnabled);

       //Set the display individual dates switch to what is in the database
       datesSwitch.setChecked(displayDatesEnabled);

       //Hide text box on load
        TextView txtNotification = (TextView) findViewById(R.id.txt_notification_minutes);
        if (!notificationEnabled){
            txtNotification.setVisibility(View.GONE);
            inputNotification.setVisibility(View.GONE);
        }
        else {
            txtNotification.setVisibility(View.VISIBLE);
            inputNotification.setVisibility(View.VISIBLE);
        }


        //Setting up views for selecting the day range
        ((RadioButton) findViewById(R.id.radioMonday)).setText(getString(R.string.monday) + " - " + getString(R.string.sunday));
        ((RadioButton) findViewById(R.id.radioTuesday)).setText(getString(R.string.tuesday) + " - " + getString(R.string.monday));
        ((RadioButton) findViewById(R.id.radioWednesday)).setText(getString(R.string.wednesday) + " - " + getString(R.string.tuesday));
        ((RadioButton) findViewById(R.id.radioThursday)).setText(getString(R.string.thursday) + " - " + getString(R.string.wednesday));
        ((RadioButton) findViewById(R.id.radioFriday)).setText(getString(R.string.friday) + " - " + getString(R.string.thursday));
        ((RadioButton) findViewById(R.id.radioSaturday)).setText(getString(R.string.saturday) + " - " + getString(R.string.friday));
        ((RadioButton) findViewById(R.id.radioSunday)).setText(getString(R.string.sunday) + " - " + getString(R.string.saturday));

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroupDay);
        if (startDay == 1)
            radioGroup.check(R.id.radioMonday);
        else if (startDay == 2)
            radioGroup.check(R.id.radioTuesday);
        else if (startDay == 3)
            radioGroup.check(R.id.radioWednesday);
        else if (startDay == 4)
            radioGroup.check(R.id.radioThursday);
        else if (startDay == 5)
            radioGroup.check(R.id.radioFriday);
        else if (startDay == 6)
            radioGroup.check(R.id.radioSaturday);
        else if (startDay == 0)
            radioGroup.check(R.id.radioSunday);

    }

    protected void onStart() {
        super.onStart();

        //Enable the Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.settings);
    }

    //Adds the check and cross icons in the toolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm_cancel, menu);
        //Change the color of the icons in action bar
        Drawable cancel = menu.getItem(0).getIcon();
        cancel.mutate();
        cancel.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        Drawable confirm = menu.getItem(1).getIcon();
        confirm.mutate();
        confirm.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        //Call super class
        return super.onCreateOptionsMenu(menu);
    }

    //Add functionality to the check and cross icons in the toolbar
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        switch (item.getItemId()) {
            case R.id.menu_confirm:

                //Database
                SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();

                //Get the number correpesonding to the selected day
                RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroupDay);
                int dayNum = 1;
                switch(radioGroup.getCheckedRadioButtonId()) {
                    case R.id.radioMonday:
                        dayNum = 1;
                        break;
                    case R.id.radioTuesday:
                        dayNum = 2;
                        break;
                    case R.id.radioWednesday:
                        dayNum = 3;
                        break;
                    case R.id.radioThursday:
                        dayNum = 4;
                        break;
                    case R.id.radioFriday:
                        dayNum = 5;
                        break;
                    case R.id.radioSaturday:
                        dayNum = 6;
                        break;
                    case R.id.radioSunday:
                        dayNum = 0;
                        break;
                }

                //Reset the planners position (the week its currently displaying) if day range has changed
                if (dayNum != startDay)
                    MainActivity.pos = -1;

                //Views
                Switch notificationSwitch = (Switch) findViewById(R.id.notification_switch);
                Switch datesSwitch = (Switch) findViewById(R.id.individual_dates_switch);
                EditText notificatioMinutes = (EditText) findViewById(R.id.input_notification_minutes);

                //Convert string to integer
                String minutes = notificatioMinutes.getText().toString();
                int mins = 0;
                try {
                    mins = Integer.parseInt(minutes);
                } catch (Exception e) {
                    mins = 0;
                }

                //Set database info
                plannerDatabaseHelper.updateSettings(db, notificationSwitch.isChecked(), dayNum, mins, datesSwitch.isChecked());
                finish();
                return true;
            case R.id.menu_cancel:
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
