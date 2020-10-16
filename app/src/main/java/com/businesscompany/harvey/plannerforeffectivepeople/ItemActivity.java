package com.businesscompany.harvey.plannerforeffectivepeople;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ItemActivity extends AppCompatActivity {

    //Global views
    DialogFragment newFragment;
    private RadioGroup radioGroup;
    private TimePicker timeStart;
    private TimePicker timeEnd;
    private TableLayout timePickerLabels;
    TextView colourView;
    TextView repeatView;
    boolean repeatWeekdayUnset = false;
    Date oldDate = new Date();
    int screenWidth = 1000;

    //Repetition Values
    String repeat_interval = "NEVER";
    int repeat_interval_number = 1;
    int ends_after = 0;
    int monthly_repeat_mode = 0;
    int weekdays = 0;


    //Database
    PlannerDatabaseHelper plannerDatabaseHelper;

    //Variables
    private int id;
    String selected_color = "orange";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        //Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_item);
        setSupportActionBar(toolbar);

        //Enable the Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("Add Item");
        toolbar.setTitleTextColor(0xFFFFFFFF);

        //Get the dimensions of the screen
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;

        //Set the date text view text colour to black
        TextView dateLabel = (TextView) findViewById(R.id.txt_date);
        dateLabel.setTextColor(Color.BLACK);

        //Set up the time picker widgets
        timeStart = (TimePicker) findViewById(R.id.simpleTimePickerStart);
        timeEnd = (TimePicker) findViewById(R.id.simpleTimePickerEnd);
        timeStart.setIs24HourView(true);
        timeEnd.setIs24HourView(true);

        //Set up the colour picker
        colourView = (TextView) findViewById(R.id.txtColour);
        ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.orange));
        colourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                onColorClick();
            }
        });


        //Hide/show logic for the time picker
        addListenerOnRadioGroup();

        //Setup repeatition picker
        repeatView = (TextView) findViewById(R.id.textRepeat);
        repeatView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_black_24dp, 0, 0, 0);
        repeatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                onRepeatClick();
            }
        });


        //Define datebase
        plannerDatabaseHelper = new PlannerDatabaseHelper(this);

        //Information about the planner item, if the bundle doesn't exist it just goes into the catch block
        try {
            Intent in = getIntent();
            Bundle bundle = in.getExtras();

            //Get database object
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();

            //Gets references to the views on the page
            TextView txtTitle = (TextView) findViewById(R.id.txt_title);
            TextView txtDescription = (TextView) findViewById(R.id.txt_description);
            DatePicker datePicker = (DatePicker) findViewById(R.id.simpleDatePicker);
            RadioButton radioPriority = (RadioButton) findViewById(R.id.radioPriority);
            RadioButton radioAppointment = (RadioButton) findViewById(R.id.radioAppointment);

            //Define the reference to the information in the planner item
            PlannerItem plannerItem;

            //Edits flag = tells the next view this is an edit of the current item and not a copy
            boolean edit_flag = bundle.getBoolean("edit_flag");
            if (edit_flag) {
                id = bundle.getInt("id");
                plannerItem = plannerDatabaseHelper.getItem(db, id); //Get data from the database
                ab.setTitle("Edit Item");

            } else {
                id = 0;
                plannerItem = plannerDatabaseHelper.getItem(db, bundle.getInt("id"));
            }

            //Text fields
            txtTitle.setText(plannerItem.getTitle());
            txtDescription.setText(plannerItem.getDescription());
            //Color picker
            selected_color = plannerItem.getColour().toLowerCase();
            //Date Picker
            oldDate = plannerItem.getStartDate();
            datePicker.init(
                plannerItem.getStartDate().getYear() + 1900,
                plannerItem.getStartDate().getMonth(),
                plannerItem.getStartDate().getDate(),
                new DatePicker.OnDateChangedListener() {
                    //if the task repeats once a week and the selected date, when date picker changes the repeat date should change too
                    @Override
                        public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {
                            Date d1 = new Date();
                            String dateString = Integer.toString(datePicker.getYear()) +
                                    '/' + Integer.toString(datePicker.getMonth() + 1) +
                                    '/' + Integer.toString(datePicker.getDayOfMonth());
                                try {
                                //Read what inputs the user has entered
                                SimpleDateFormat sdf;
                                sdf = new SimpleDateFormat("yyyy/MM/dd"); // here set the pattern as you date in string was containing like date/month/year
                                d1 = sdf.parse(dateString);
                            } catch (ParseException ex) {
                                d1 = new Date();
                            }

                            if ((oldDate.getDay() == 0 && weekdays == 64) ||
                                (oldDate.getDay() == 1 && weekdays == 1) ||
                                (oldDate.getDay() == 2 && weekdays == 2) ||
                                (oldDate.getDay() == 3 && weekdays == 4) ||
                                (oldDate.getDay() == 4 && weekdays == 8) ||
                                (oldDate.getDay() == 5 && weekdays == 16) ||
                                (oldDate.getDay() == 6 && weekdays == 32)) {
                                    changeWeekdays(d1.getDay());
                            }
                        }

                        public void changeWeekdays(int newWeekday){
                            if (newWeekday == 0)
                                weekdays = 64;
                            else if (newWeekday == 1)
                                weekdays = 1;
                            else if (newWeekday == 2)
                                weekdays = 2;
                            else if (newWeekday == 3)
                                weekdays = 4;
                            else if (newWeekday == 4)
                                weekdays = 8;
                            else if (newWeekday == 5)
                                weekdays = 16;
                            else if (newWeekday == 6)
                                weekdays = 32;
                        }
                    }
                );
            //Time Pickers
            if (plannerItem.getPriority() == 0) {
                radioAppointment.toggle();
                timeStart.setCurrentHour(plannerItem.getStartDate().getHours());
                timeStart.setCurrentMinute(plannerItem.getStartDate().getMinutes());
                timeEnd.setCurrentHour(plannerItem.getEndDate().getHours());
                timeEnd.setCurrentMinute(plannerItem.getEndDate().getMinutes());
            }

            //Repetition data
            repeat_interval = plannerItem.getRepeat_interval();
            repeat_interval_number = plannerItem.getRepeat_interval_number();
            ends_after = plannerItem.getEnds_after();
            monthly_repeat_mode = plannerItem.getMonthly_repeat_mode();
            weekdays = plannerItem.getWeekdays();


            db.close();
        } catch (Exception e) {
            id = 0;
        }

        //set text of repeat button on main item page
        if (repeat_interval_number == 1 || repeat_interval.equals("NEVER")) {
            repeatView.setText(repeat_interval.substring(0, 1) + repeat_interval.substring(1).toLowerCase());
        }
        else if (repeat_interval.equals("DAILY")){
            repeatView.setText(getString(R.string.every) + " " + repeat_interval_number + " " + getString(R.string.day_plural));
        }
        else if (repeat_interval.equals("WEEKLY")){
            repeatView.setText(getString(R.string.every) + " " + repeat_interval_number + " " + getString(R.string.week_plural));
        }
        else if (repeat_interval.equals("MONTHLY")){
            repeatView.setText(getString(R.string.every) + " " + repeat_interval_number + " " + getString(R.string.month_plural));
        }
        else {
            repeatView.setText(getString(R.string.every) + " " + repeat_interval_number + " " + getString(R.string.year_plural));
        }

        setPickersColor();
    }

    //The on click listener for the Priority/Appointments radio button
    public void addListenerOnRadioGroup() {
        //Get views
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        timePickerLabels = (TableLayout) findViewById(R.id.timePickerLabels);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioPriority) {
                    timeStart.setVisibility(View.GONE);
                    timeEnd.setVisibility(View.GONE);
                    timePickerLabels.setVisibility(View.GONE);
                } else if (checkedId == R.id.radioAppointment) {
                    timeStart.setVisibility(View.VISIBLE);
                    timeEnd.setVisibility(View.VISIBLE);
                    timePickerLabels.setVisibility(View.VISIBLE);
                }
            }
        });

        RadioButton radioPriority = (RadioButton) findViewById(R.id.radioPriority);
        radioPriority.setChecked(true);
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
                //Views containing the input data
                EditText title = (EditText) findViewById(R.id.txt_title);
                EditText description = (EditText) findViewById(R.id.txt_description);
                RadioButton priority = (RadioButton) findViewById(R.id.radioPriority);
                RadioButton appointment = (RadioButton) findViewById(R.id.radioAppointment);
                TextView date = (TextView) findViewById(R.id.txt_date);
                DatePicker datePicker = (DatePicker) findViewById(R.id.simpleDatePicker);

                Date d1, d2;
                String dateString = Integer.toString(datePicker.getYear()) +
                        '/' + Integer.toString(datePicker.getMonth() + 1) +
                        '/' + Integer.toString(datePicker.getDayOfMonth());
                try {
                    //Read what inputs the user has entered
                    SimpleDateFormat sdf;
                    if (priority.isChecked()) {
                        sdf = new SimpleDateFormat("yyyy/MM/dd"); // here set the pattern as you date in string was containing like date/month/year
                        d1 = sdf.parse(dateString);
                        d2 = d1;
                    } else {
                        String time1 = Integer.toString(timeStart.getCurrentHour()) +
                                ':' + Integer.toString(timeStart.getCurrentMinute());
                        String time2 = Integer.toString(timeEnd.getCurrentHour()) +
                                ':' + Integer.toString(timeEnd.getCurrentMinute());
                        sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                        d1 = sdf.parse(dateString + ' ' + time1);
                        d2 = sdf.parse(dateString + ' ' + time2);
                    }
                } catch (ParseException ex) {
                    d1 = new Date();
                    d2 = new Date();
                }

                //Double check weekday variable is all good (just in case it was left unset in the dialog)
                if (repeatWeekdayUnset)
                    if (d1.getDay() == 0)
                        weekdays = 64;
                    else if (d1.getDay() == 1)
                        weekdays = 1;
                    else if (d1.getDay() == 2)
                        weekdays = 2;
                    else if (d1.getDay() == 3)
                        weekdays = 4;
                    else if (d1.getDay() == 4)
                        weekdays = 8;
                    else if (d1.getDay() == 5)
                        weekdays = 16;
                    else if (d1.getDay() == 6)
                        weekdays = 32;

                //Define a new object (just for convenience) to hold input data
                String t = title.getText().toString();
                if (t.isEmpty()){
                    t = " ";
                }

                PlannerItem newItem = new PlannerItem(
                        id,
                        t,
                        description.getText().toString(),
                        priority.isChecked(),
                        d1,
                        d2,
                        selected_color,
                        0,
                        repeat_interval,
                        repeat_interval_number,
                        ends_after,
                        monthly_repeat_mode,
                        weekdays
                );

                //Insert new item into the database
                try {
                    SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
                    if (id == 0)
                        plannerDatabaseHelper.insertItem(db, newItem);
                    else
                        plannerDatabaseHelper.updateItem(db, newItem);
                    db.close();
                } catch (SQLiteException e) {
                    Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
                    toast.show();
                }



                //Close the page
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.menu_cancel:
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void setPickersColor(){
        ((GradientDrawable) colourView.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        if (selected_color.equals("white")) {
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.white));
            ((GradientDrawable) colourView.getBackground()).setStroke(2, getResources().getColor(R.color.grey));
        } else if (selected_color.equals("red")) {
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.red));
        } else if (selected_color.equals("yellow")) {
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.yellow));
        } else if (selected_color.equals("green")) {
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.green));
        } else if (selected_color.equals("cyan")){
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.cyan));
        } else if (selected_color.equals("purple")){
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.purple));
        } else if (selected_color.equals("pink")){
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.pink));
        } else if (selected_color.equals("blue")){
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.blue));
        } else {
            ((GradientDrawable) colourView.getBackground()).setColor(ContextCompat.getColor(this, R.color.orange));
        }
        colourView.getBackground().clearColorFilter();
    }

    public void onRepeatClick() {
        //Custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.repeat_picker);
        dialog.setTitle("Repeat");

        //Show the repeat dialog
        dialog.show();

        //get current selected date
        DatePicker datePicker = (DatePicker) findViewById(R.id.simpleDatePicker);

        //Define the views
        RadioGroup radGroupRepetitionTime = (RadioGroup) dialog.findViewById(R.id.radioGroupRepetitionTime);
        RadioGroup radGroupEveryXDay = (RadioGroup) dialog.findViewById(R.id.radGroupEveryXDay);
        Switch switch_end = (Switch) dialog.findViewById(R.id.switch_end);
        EditText input_repeatition_interval = (EditText) dialog.findViewById(R.id.input_repeatition_interval);
        EditText input_repeatition_number = (EditText) dialog.findViewById(R.id.input_repeatition_number);
        ToggleButton tglMon = (ToggleButton) dialog.findViewById(R.id.tglMon);
        ToggleButton tglTue = (ToggleButton) dialog.findViewById(R.id.tglTue);
        ToggleButton tglWed = (ToggleButton) dialog.findViewById(R.id.tglWed);
        ToggleButton tglThu = (ToggleButton) dialog.findViewById(R.id.tglThu);
        ToggleButton tglFri = (ToggleButton) dialog.findViewById(R.id.tglFri);
        ToggleButton tglSat = (ToggleButton) dialog.findViewById(R.id.tglSat);
        ToggleButton tglSun = (ToggleButton) dialog.findViewById(R.id.tglSun);
        RadioButton radEveryXDay = (RadioButton) dialog.findViewById(R.id.radEveryXDay);
        RadioButton radEveryXDayOfWeek = (RadioButton) dialog.findViewById(R.id.radEveryXDayOfWeek);

        //Initalize values for the views
        switch (repeat_interval) {
            case "NEVER":
                radGroupRepetitionTime.check(R.id.radioNever);
                break;
            case "DAILY":
                radGroupRepetitionTime.check(R.id.radioDaily);
                break;
            case "WEEKLY":
                radGroupRepetitionTime.check(R.id.radioWeekly);
                if ((1 & weekdays) == 1)
                    tglMon.setChecked(true);
                if ((2 & weekdays) == 2)
                    tglTue.setChecked(true);
                if ((4 & weekdays) == 4)
                    tglWed.setChecked(true);
                if ((8 & weekdays) == 8)
                    tglThu.setChecked(true);
                if ((16 & weekdays) == 16)
                    tglFri.setChecked(true);
                if ((32 & weekdays) == 32)
                    tglSat.setChecked(true);
                if ((64 & weekdays) == 64)
                    tglSun.setChecked(true);
                break;
            case "MONTHLY":
                radGroupRepetitionTime.check(R.id.radioMonthly);
                if (monthly_repeat_mode == 0)
                    radGroupEveryXDay.check(R.id.radEveryXDay);
                else
                    radGroupEveryXDay.check(R.id.radEveryXDayOfWeek);
                break;
            case "YEARLY":
                radGroupRepetitionTime.check(R.id.radioYearly);
                break;
        }

        //Set the text of various views
        Date d_temp;
        final Date d1;
        try {
            //Read what inputs the user has entered
            String dateString = Integer.toString(datePicker.getYear()) +
                        '/' + Integer.toString(datePicker.getMonth() + 1) +
                        '/' + Integer.toString(datePicker.getDayOfMonth());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd"); // here set the pattern as you date in string was containing like date/month/year
            d_temp = sdf.parse(dateString);
        }
        catch (Exception e){
            d_temp = new Date();
        }
        d1 = d_temp;

        String dayString;
        //If all weekly toggle buttons are turned off, turn on the current day selected
        boolean noTogglesOn = !tglSun.isChecked() && !tglMon.isChecked() && !tglTue.isChecked() && !tglWed.isChecked() && !tglThu.isChecked() && !tglFri.isChecked() && !tglSat.isChecked();
        if (d1.getDay() == 0) {
            dayString = getString(R.string.sunday);
            if (noTogglesOn)
                tglSun.setChecked(true);
        }
        else if (d1.getDay() == 1) {
            dayString = getString(R.string.monday);
            if (noTogglesOn)
                tglMon.setChecked(true);
        }
        else if (d1.getDay() == 2) {
            dayString = getString(R.string.tuesday);
            if (noTogglesOn)
                tglTue.setChecked(true);
        }
        else if (d1.getDay() == 3) {
            dayString = getString(R.string.wednesday);
            if (noTogglesOn)
                tglWed.setChecked(true);
        }
        else if (d1.getDay() == 4) {
            dayString = getString(R.string.thursday);
            if (noTogglesOn)
                tglThu.setChecked(true);
        }
        else if (d1.getDay() == 5) {
            dayString = getString(R.string.friday);
            if (noTogglesOn)
                tglFri.setChecked(true);
        }
        else{
            dayString = getString(R.string.saturday);
            if (noTogglesOn)
                tglSat.setChecked(true);
        }
        int count = 0;
        Calendar c_count = Calendar.getInstance();
        c_count.setTime(d1);
        c_count.set(Calendar.DAY_OF_MONTH, 1);
        for (int i = 0; i < datePicker.getDayOfMonth(); i++){
            if ((c_count.get(Calendar.DAY_OF_WEEK) - 1) == d1.getDay())
                count++;
            c_count.add(Calendar.DATE, 1);
        }

        if (count <= 4)
            radEveryXDayOfWeek.setText(getString(R.string.every) + " " + ordinal(count) + " " + dayString);
        else
            radEveryXDayOfWeek.setText(getString(R.string.every) + " " + getString(R.string.last) + " " + dayString);
        radEveryXDay.setText(getString(R.string.every) +  " " + ordinal(datePicker.getDayOfMonth()) + " " + getString(R.string.day));
        input_repeatition_interval.setText(Integer.toString(repeat_interval_number));

        if (ends_after == 0) {
            switch_end.setChecked(false);
            input_repeatition_number.setText("1");
        }
        else {
            switch_end.setChecked(true);
            input_repeatition_number.setText(Integer.toString(ends_after));
        }

        //set up the cancel button
        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                repeatView.getBackground().clearColorFilter();
            }
        });

        //Set the okay button, this should save the details somewhere on in this activity
        Button btnOk = (Button) dialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Save the data on the dialog
                //Define the views
                RadioGroup radGroupRepetitionTime = (RadioGroup) dialog.findViewById(R.id.radioGroupRepetitionTime);
                RadioGroup radGroupEveryXDay = (RadioGroup) dialog.findViewById(R.id.radGroupEveryXDay);
                Switch switch_end = (Switch) dialog.findViewById(R.id.switch_end);
                final EditText input_repeatition_interval = (EditText) dialog.findViewById(R.id.input_repeatition_interval);
                final EditText input_repeatition_number = (EditText) dialog.findViewById(R.id.input_repeatition_number);
                ToggleButton tglMon = (ToggleButton) dialog.findViewById(R.id.tglMon);
                ToggleButton tglTue = (ToggleButton) dialog.findViewById(R.id.tglTue);
                ToggleButton tglWed = (ToggleButton) dialog.findViewById(R.id.tglWed);
                ToggleButton tglThu = (ToggleButton) dialog.findViewById(R.id.tglThu);
                ToggleButton tglFri = (ToggleButton) dialog.findViewById(R.id.tglFri);
                ToggleButton tglSat = (ToggleButton) dialog.findViewById(R.id.tglSat);
                ToggleButton tglSun = (ToggleButton) dialog.findViewById(R.id.tglSun);

                //Save top radio buttons
                switch (radGroupRepetitionTime.getCheckedRadioButtonId()) {
                    case R.id.radioNever:
                        repeat_interval = "NEVER";
                        break;
                    case R.id.radioDaily:
                        repeat_interval = "DAILY";
                        break;
                    case R.id.radioWeekly:
                        repeat_interval = "WEEKLY";
                        break;
                    case R.id.radioMonthly:
                        repeat_interval = "MONTHLY";
                        break;
                    case R.id.radioYearly:
                        repeat_interval = "YEARLY";
                        break;
                }

                //Save how many repeats should be done
                int how_often;
                try {
                    how_often = Integer.parseInt(input_repeatition_interval.getText().toString());
                }
                catch (Exception e) {
                    how_often = 1;
                }
                if (how_often <= 0)
                    how_often = 1;
                repeat_interval_number = how_often;

                //Save which days of the week to repeat on
                weekdays = 0;
                if (repeat_interval.equals("WEEKLY")){
                    if (tglMon.isChecked())
                        weekdays |= 1;
                    if (tglTue.isChecked())
                        weekdays |= 2;
                    if (tglWed.isChecked())
                        weekdays |= 4;
                    if (tglThu.isChecked())
                        weekdays |= 8;
                    if (tglFri.isChecked())
                        weekdays |= 16;
                    if (tglSat.isChecked())
                        weekdays |= 32;
                    if (tglSun.isChecked())
                        weekdays |= 64;
                }
                //if not set deault to current day of the week
                repeatWeekdayUnset = false;
                if (weekdays == 0){
                    repeatWeekdayUnset = true;
                    if (d1.getDay() == 0)
                        weekdays = 64;
                    else if (d1.getDay() == 1)
                        weekdays = 1;
                    else if (d1.getDay() == 2)
                        weekdays = 2;
                    else if (d1.getDay() == 3)
                        weekdays = 4;
                    else if (d1.getDay() == 4)
                        weekdays = 8;
                    else if (d1.getDay() == 5)
                        weekdays = 16;
                    else if (d1.getDay() == 6)
                        weekdays = 32;
                }

                //Save how many repeats should be done
                if (!switch_end.isChecked())
                    ends_after = 0;
                else{
                    try {
                        ends_after = Integer.parseInt(input_repeatition_number.getText().toString());
                    }
                    //Should be a NumberFormatException
                    catch (Exception e) {
                        ends_after = 0;
                    }
                }

                //Save how repeations should be done if the monthly option is selected
                if (repeat_interval.equals("MONTHLY")){
                    switch (radGroupEveryXDay.getCheckedRadioButtonId()) {
                        case R.id.radEveryXDay:
                            monthly_repeat_mode = 0;
                            break;
                        case R.id.radEveryXDayOfWeek:
                            monthly_repeat_mode = 1;
                            break;
                    }
                }

                //set text of repeat button on main item page
                if (repeat_interval_number == 1 || repeat_interval.equals("NEVER")) {
                    repeatView.setText(repeat_interval.substring(0, 1) + repeat_interval.substring(1).toLowerCase());
                }
                else {
                    TextView txt_repeatition_interval = (TextView) dialog.findViewById(R.id.txt_repeatition_interval);
                    repeatView.setText(getString(R.string.every) + " " + repeat_interval_number + " " + txt_repeatition_interval.getText().toString());
                }

                //Close the dialog
                dialog.dismiss();
                repeatView.getBackground().clearColorFilter();
            }
        });

        //When dialog is closed change the color of the Repeat button on the items page
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                repeatView.getBackground().clearColorFilter();
            }
        });

        //Hide/show logic of the repeat dialog
        repeatHideShowLogic(dialog);
        RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radioGroupRepetitionTime);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                repeatHideShowLogic(dialog);
            }
        });

        //Display logic for the EditTexts
        input_repeatition_interval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                repeatHideShowLogic(dialog);
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        input_repeatition_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                repeatHideShowLogic(dialog);
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        //Hide/Show logic for the switch
        switch_end.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LinearLayout inputEndsAfter = (LinearLayout) dialog.findViewById(R.id.inputEndsAfter);
                if (isChecked)
                    inputEndsAfter.setVisibility(View.VISIBLE);
                else
                    inputEndsAfter.setVisibility(View.GONE);

            }
        });

        //For smaller screen sizes, make the buttons smaller
        if (screenWidth < 768){
            //Hard coded pixel size values, not good, maybe fix this later
            btnCancel.setLayoutParams(new LinearLayout.LayoutParams(105, LinearLayout.LayoutParams.WRAP_CONTENT));
            btnOk.setLayoutParams(new LinearLayout.LayoutParams(105, LinearLayout.LayoutParams.WRAP_CONTENT));

            tglMon.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglTue.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglWed.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglThu.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglFri.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglSat.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));
            tglSun.setLayoutParams(new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT));

        }

    }

    public void repeatHideShowLogic(Dialog d){

        //Hide/show logic for the views on the repeat dialog
        RadioGroup radioGroup = (RadioGroup) d.findViewById(R.id.radioGroupRepetitionTime);

        LinearLayout inputTimePeriod = (LinearLayout) d.findViewById(R.id.inputTimePeriod);
        LinearLayout inputDayPicker = (LinearLayout) d.findViewById(R.id.inputDayPicker);
        RadioGroup radGroupEveryXDay = (RadioGroup) d.findViewById(R.id.radGroupEveryXDay);
        Switch switch_end = (Switch) d.findViewById(R.id.switch_end);
        LinearLayout inputEndsAfter = (LinearLayout) d.findViewById(R.id.inputEndsAfter);
        EditText input_repeatition_interval = (EditText) d.findViewById(R.id.input_repeatition_interval);
        EditText input_repeatition_number = (EditText) d.findViewById(R.id.input_repeatition_number);
        TextView txt_repeatition_interval = (TextView) d.findViewById(R.id.txt_repeatition_interval);
        TextView txt_repeatition_number = (TextView) d.findViewById(R.id.txt_repeatition_number);
        View seperator_1 = (View) d.findViewById(R.id.seperator_1);
        View seperator_2 = (View) d.findViewById(R.id.seperator_2);

        //Converting strings into integers
        int repetitionInterval = 1;
        int repetionNumber = 1;
        try {
            repetitionInterval = Integer.parseInt(input_repeatition_interval.getText().toString());
        }
        catch (Exception e) { }

        try {
            repetionNumber = Integer.parseInt(input_repeatition_number.getText().toString());
        }
        catch (Exception e) { }

        //Hide and show view based on the first set oif radio buttons
        switch(radioGroup.getCheckedRadioButtonId()) {
            case R.id.radioNever:
                inputTimePeriod.setVisibility(View.GONE);
                inputDayPicker.setVisibility(View.GONE);
                radGroupEveryXDay.setVisibility(View.GONE);
                switch_end.setVisibility(View.GONE);
                inputEndsAfter.setVisibility(View.GONE);
                seperator_1.setVisibility(View.GONE);
                seperator_2.setVisibility(View.GONE);

                break;
            case R.id.radioDaily:
                inputTimePeriod.setVisibility(View.VISIBLE);
                inputDayPicker.setVisibility(View.GONE);
                radGroupEveryXDay.setVisibility(View.GONE);
                switch_end.setVisibility(View.VISIBLE);
                inputEndsAfter.setVisibility(View.VISIBLE);
                seperator_1.setVisibility(View.VISIBLE);
                seperator_2.setVisibility(View.VISIBLE);
                if (repetitionInterval > 1)
                    txt_repeatition_interval.setText(getString(R.string.day_plural));
                else
                    txt_repeatition_interval.setText(getString(R.string.day));
                break;
            case R.id.radioWeekly:
                inputTimePeriod.setVisibility(View.VISIBLE);
                inputDayPicker.setVisibility(View.VISIBLE);
                radGroupEveryXDay.setVisibility(View.GONE);
                switch_end.setVisibility(View.VISIBLE);
                inputEndsAfter.setVisibility(View.VISIBLE);
                seperator_1.setVisibility(View.VISIBLE);
                seperator_2.setVisibility(View.VISIBLE);
                if (repetitionInterval > 1)
                    txt_repeatition_interval.setText(getString(R.string.week_plural));
                else
                    txt_repeatition_interval.setText(getString(R.string.week));
                break;
            case R.id.radioMonthly:
                inputTimePeriod.setVisibility(View.VISIBLE);
                inputDayPicker.setVisibility(View.GONE);
                radGroupEveryXDay.setVisibility(View.VISIBLE);
                switch_end.setVisibility(View.VISIBLE);
                inputEndsAfter.setVisibility(View.VISIBLE);
                seperator_1.setVisibility(View.VISIBLE);
                seperator_2.setVisibility(View.VISIBLE);
                if (repetitionInterval > 1)
                    txt_repeatition_interval.setText(getString(R.string.month_plural));
                else
                    txt_repeatition_interval.setText(getString(R.string.month));
                break;
            case R.id.radioYearly:
                inputTimePeriod.setVisibility(View.VISIBLE);
                inputDayPicker.setVisibility(View.GONE);
                radGroupEveryXDay.setVisibility(View.GONE);
                switch_end.setVisibility(View.VISIBLE);
                inputEndsAfter.setVisibility(View.VISIBLE);
                seperator_1.setVisibility(View.VISIBLE);
                seperator_2.setVisibility(View.VISIBLE);
                if (repetitionInterval > 1)
                    txt_repeatition_interval.setText(getString(R.string.year_plural));
                else
                    txt_repeatition_interval.setText(getString(R.string.year));
                break;
        }


        //Changes text box to be display the plural or singular word depending on the value of whats in the edit text
        if (repetionNumber > 1)
            txt_repeatition_number.setText(getString(R.string.repetition_plural));
        else
            txt_repeatition_number.setText(getString(R.string.repetition));
        //Hides or shows the Ends After EditText
        if (switch_end.isChecked() && radioGroup.getCheckedRadioButtonId() != R.id.radioNever)
            inputEndsAfter.setVisibility(View.VISIBLE);
        else
            inputEndsAfter.setVisibility(View.GONE);


        //set the dimensions of the dialog
        Window window = d.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    }

    public void onColorClick() {

        //Custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.color_picker);
        dialog.setTitle("Pick a Colour");

        //Set the colour of the text views
        TextView white = (TextView) dialog.findViewById(R.id.color_white);
        if (white.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) white.getBackground()).setColor(ContextCompat.getColor(this, R.color.white));
            ((GradientDrawable) white.getBackground()).setStroke(2, getResources().getColor(R.color.grey));
        }
        TextView orange = (TextView) dialog.findViewById(R.id.color_orange);
        if (orange.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) orange.getBackground()).setColor(ContextCompat.getColor(this, R.color.orange));
            ((GradientDrawable) orange.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView red = (TextView) dialog.findViewById(R.id.color_red);
        if (red.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) red.getBackground()).setColor(ContextCompat.getColor(this, R.color.red));
            ((GradientDrawable) red.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView cyan = (TextView) dialog.findViewById(R.id.color_cyan);
        if (cyan.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) cyan.getBackground()).setColor(ContextCompat.getColor(this, R.color.cyan));
            ((GradientDrawable) cyan.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView yellow = (TextView) dialog.findViewById(R.id.color_yellow);
        if (yellow.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) yellow.getBackground()).setColor(ContextCompat.getColor(this, R.color.yellow));
            ((GradientDrawable) yellow.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView green = (TextView) dialog.findViewById(R.id.color_green);
        if (green.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) green.getBackground()).setColor(ContextCompat.getColor(this, R.color.green));
            ((GradientDrawable) green.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView pink = (TextView) dialog.findViewById(R.id.color_pink);
        if (pink.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) pink.getBackground()).setColor(ContextCompat.getColor(this, R.color.pink));
            ((GradientDrawable) pink.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView purple = (TextView) dialog.findViewById(R.id.color_purple);
        if (purple.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) purple.getBackground()).setColor(ContextCompat.getColor(this, R.color.purple));
            ((GradientDrawable) purple.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }
        TextView blue = (TextView) dialog.findViewById(R.id.color_blue);
        if (blue.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) blue.getBackground()).setColor(ContextCompat.getColor(this, R.color.blue));
            ((GradientDrawable) blue.getBackground()).setStroke(2, getResources().getColor(R.color.white));
        }

        //Set the on click listeners of the text views
        white.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "white";
                dialog.dismiss();
                setPickersColor();
            }
        });
        orange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "orange";
                dialog.dismiss();
                setPickersColor();
            }
        });
        red.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "red";
                dialog.dismiss();
                setPickersColor();
            }
        });
        cyan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "cyan";
                dialog.dismiss();
                setPickersColor();
            }
        });
        yellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "yellow";
                dialog.dismiss();
                setPickersColor();
            }
        });
        green.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "green";
                dialog.dismiss();
                setPickersColor();
            }
        });
        purple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "purple";
                dialog.dismiss();
                setPickersColor();
            }
        });
        pink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "pink";
                dialog.dismiss();
                setPickersColor();
            }
        });
        blue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);
                selected_color = "blue";
                dialog.dismiss();
                setPickersColor();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                colourView.getBackground().clearColorFilter();
            }
        });
        dialog.show();


    }

    public static String ordinal(int i) {
        String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + sufixes[i % 10];

        }
    }

}