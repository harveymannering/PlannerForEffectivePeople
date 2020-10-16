package com.businesscompany.harvey.plannerforeffectivepeople;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PlannerFragment extends Fragment {


    PlannerDatabaseHelper plannerDatabaseHelper;
    int width;
    int tableRows = 0;
    View view;
    int index;
    Date startDate = new Date();
    Date endDate = new Date();
    int dayRange = 1;//1 = Monday - Sunday
                     //2 = Tuesday - Monday
                     //...

    public PlannerFragment(){
        index = 0;
    }

    @SuppressLint("ValidFragment")
    public PlannerFragment(int index, Date startDate, Date endDate){
        this.index = index;
        this.startDate = startDate;

        Calendar c = Calendar.getInstance();
        c.setTime(endDate);
        c.add(Calendar.SECOND, -1);
        this.endDate = c.getTime();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_planner, container, false);

        //Setting up the Floating action button used to add a note
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_add_item);
        fab.setOnClickListener(new PlannerFragment.AddItemClickListener());

        //Define datebase
        plannerDatabaseHelper = new PlannerDatabaseHelper(getContext());

        boolean displayDatesEnabled = false;
        //Find settings relevant to this page
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            displayDatesEnabled = plannerDatabaseHelper.isSettingDisplayed(db, "DISPLAY_DATES");
            dayRange = plannerDatabaseHelper.getStartDay(db);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(getActivity(), "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Write the names of the days at the top of the page e.g. Wednesday to Tuesday
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar c1 = Calendar.getInstance();
        c1.setTime(startDate);
        labelDayColumn(0, (TextView) view.findViewById(R.id.day1label), (TextView) view.findViewById(R.id.date1label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(1, (TextView) view.findViewById(R.id.day2label), (TextView) view.findViewById(R.id.date2label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(2, (TextView) view.findViewById(R.id.day3label), (TextView) view.findViewById(R.id.date3label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(3, (TextView) view.findViewById(R.id.day4label), (TextView) view.findViewById(R.id.date4label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(4, (TextView) view.findViewById(R.id.day5label), (TextView) view.findViewById(R.id.date5label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(5, (TextView) view.findViewById(R.id.day6label), (TextView) view.findViewById(R.id.date6label), displayDatesEnabled, dateFormat1, c1);
        labelDayColumn(6, (TextView) view.findViewById(R.id.day7label), (TextView) view.findViewById(R.id.date7label), displayDatesEnabled, dateFormat1, c1);

        //Write the date at the top of the page
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Calendar c2 = Calendar.getInstance();
        c2.setTime(endDate);
        TextView dateLabel = (TextView) view.findViewById(R.id.dateLabel);
        dateLabel.setText(dateFormat2.format(startDate) + " to " + dateFormat2.format(c2.getTime()));

        //Draw the actual planner
        tableRows = 3;
        drawPlanner();
        drawSchedule();

        return view;
    }

    public void labelDayColumn(int count, TextView dayLabel, TextView dateLabel, boolean displayDates, SimpleDateFormat dateFormat, Calendar c){



        int dayNumber = dayRange + count ;
        if (dayNumber > 6)
            dayNumber -= 7;

        if (dayNumber == 0)
            dayLabel.setText(getString(R.string.sun));
        else if (dayNumber == 1)
            dayLabel.setText(getString(R.string.mon));
        else if (dayNumber == 2)
            dayLabel.setText(getString(R.string.tue));
        else if (dayNumber == 3)
            dayLabel.setText(getString(R.string.wed));
        else if (dayNumber == 4)
            dayLabel.setText(getString(R.string.thu));
        else if (dayNumber == 5)
            dayLabel.setText(getString(R.string.fri));
        else if (dayNumber == 6)
            dayLabel.setText(getString(R.string.sat));

        float scale = getResources().getDisplayMetrics().density;

        if (displayDates) {
            dayLabel.setPadding(0, (int) (10 * scale + 0.5f), 0, (int) (0 + 0.5f));
            dateLabel.setVisibility(View.VISIBLE);
            dateLabel.setText(dateFormat.format(c.getTime()));
            c.add(Calendar.DATE, 1);
        }
        else {
            dayLabel.setPadding(0, (int) (10 * scale + 0.5f), 0, (int) (10 * scale + 0.5f));
        }

    }

    //Draws the planner and items on launch
    public void drawPlanner(){
        //Get planner items
        List<PlannerItem> plannerItems = new ArrayList<>();
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            dayRange = plannerDatabaseHelper.getStartDay(db);
            plannerItems = plannerDatabaseHelper.getItems(db, 1, startDate, endDate);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(getActivity(), "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Priorities array list
        ArrayList<ArrayList<PlannerItem>> sundayToSaturday = new ArrayList<ArrayList<PlannerItem>>(); //index 0 = sunday and index 6 = saturday (for the top level arraylist)
                                                                                                      //The second level ArrayList contains planner items i.e. the appointments and all their data
        //Define empty arrayLists
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());

        //Sort the Priorities items into days
        for (PlannerItem item : plannerItems) {
            int day = item.getStartDate().getDay();
            sundayToSaturday.get(day).add(item);
        }

        //For the day with the most Priorities items, how many items are there?
        int maxLength = Math.max(
                sundayToSaturday.get(1).size(), Math.max(
                sundayToSaturday.get(2).size(), Math.max(
                sundayToSaturday.get(3).size(), Math.max(
                sundayToSaturday.get(4).size(), Math.max(
                sundayToSaturday.get(5).size(), Math.max(
                sundayToSaturday.get(6).size(),
                sundayToSaturday.get(0).size())))))
        );

        //Get screen dimensions
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        //Define table view
        TableLayout table = (TableLayout) view.findViewById(R.id.plannerTable);
        TableRow row = new TableRow(getContext());

        //Linear Layouts, each one corresponds to a column
        int remainder = width % 7;
        int columnCounter = dayRange;

        do {
            addColumn(row, sundayToSaturday.get(columnCounter), false, remainder);
            columnCounter++;
            if (columnCounter == 7)
                columnCounter = 0;
        } while (columnCounter != dayRange);

        table.addView(row, tableRows);

        //Add the priorities items to the table
        /*for (int x = 0; x < maxLength; x++) {
            //A single row in the table
            TableRow row = new TableRow(getContext());
            row.setBackgroundColor(0xFFFFFFFF);

            //Check out day ArrayList in order to build the table row
            addLabelToRow(row, monday, x, false);
            addLabelToRow(row, tuesday, x, false);
            addLabelToRow(row, wednesday, x, false);
            addLabelToRow(row, thursday, x, false);
            addLabelToRow(row, friday, x, false);
            addLabelToRow(row, saturday, x, false);
            addLabelToRow(row, sunday, x, false);

            //Add the row to the table
            table.addView(row,3 + x);
            tableRows = x + 1;
        }*/

        tableRows += 3;
    }

    //Draws the planner and items on launch
    public void drawSchedule() {
        //Get planner items
        List<PlannerItem> plannerItems = new ArrayList<>();
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            plannerItems = plannerDatabaseHelper.getItems(db, 0, startDate, endDate);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(getActivity(), "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Sort the arraylist
        Collections.sort(plannerItems);

        //Appointments array list
        ArrayList<ArrayList<PlannerItem>> sundayToSaturday = new ArrayList<ArrayList<PlannerItem>>(); //index 0 = sunday and index 6 = saturday (for the top level arraylist)
                                                                                                      //The second level ArrayList contains planner items i.e. the appointments and all their data


        //Define empty arrayLists (avoids IndexOutOfBoundsException)
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());
        sundayToSaturday.add(new ArrayList<PlannerItem>());

        //Sort the appointments items into days
        for (PlannerItem item : plannerItems) {
            int day = item.getStartDate().getDay();
            sundayToSaturday.get(day).add(item);
        }

        //For the day with the most appointments items, how many items are there?
        int maxLength = Math.max(
                    sundayToSaturday.get(1).size(), Math.max(
                    sundayToSaturday.get(2).size(), Math.max(
                    sundayToSaturday.get(3).size(), Math.max(
                    sundayToSaturday.get(4).size(), Math.max(
                    sundayToSaturday.get(5).size(), Math.max(
                    sundayToSaturday.get(6).size(),
                    sundayToSaturday.get(0).size())))))
        );

        //Get screen dimensions
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        //Define table view
        TableLayout table = (TableLayout) view.findViewById(R.id.plannerTable);
        TableRow row = new TableRow(getContext());

        //Linear Layouts, each one corresponds to a column
        int remainder = width % 7;
        int columnCounter = dayRange;

        do {
            addColumn(row, sundayToSaturday.get(columnCounter), true, remainder);
            columnCounter++;
            if (columnCounter == 7)
                columnCounter = 0;
        } while (columnCounter != dayRange);

        table.addView(row,tableRows);

        //Add the appointments items to the table
        /*for (int x = 0; x < maxLength; x++) {
            //A single row in the table
            TableRow row = new TableRow(getContext());
            row.setBackgroundColor(0xFFFFFFFF);

            //Check out day ArrayList in order to build the table row
            addLabelToRow(row, monday, x, true);
            addLabelToRow(row, tuesday, x, true);
            addLabelToRow(row, wednesday, x, true);
            addLabelToRow(row, thursday, x, true);
            addLabelToRow(row, friday, x, true);
            addLabelToRow(row, saturday, x, true);
            addLabelToRow(row, sunday, x, true);

            //Add the row to the table
            table.addView(row,tableRows + x);
        }*/
    }

    public void addColumn(TableRow row, final ArrayList<PlannerItem> day, boolean addTime, int remainder){

        //Calculate the width of the column
        int w = width / 7;
        if (remainder > 0){
            w++;
            remainder--;
        }

        //The main view (vertical LinearLayout)
        LinearLayout dayLayout = new LinearLayout(getContext());
        dayLayout.setLayoutParams(new TableRow.LayoutParams(w, TableLayout.LayoutParams.WRAP_CONTENT));
        dayLayout.setOrientation(LinearLayout.VERTICAL);

        for (final PlannerItem item : day) {
            //The views that contain the text
            TextView label = new TextView(getContext());

            //Add data to the label
            //Set the text of the label
            if (addTime) {
                //Formate the time as a string
                String timeString = getTimeString(item.getStartDate()) + " - " + getTimeString(item.getEndDate()) + "\n";

                SpannableString text = new SpannableString(timeString + item.getTitle());
                StyleSpan boldSpan = new StyleSpan(Typeface.ITALIC);
                text.setSpan(boldSpan, 0, 13, 0);
                label.setText(text);
            } else {
                label.setText(item.getTitle());
            }
            label.setTextColor(0xFF000000);

            //Set the colours of the label
            label.setBackground(getResources().getDrawable(R.drawable.item_back));
            Drawable background = label.getBackground();
            if (background instanceof GradientDrawable) {
                // Color of the background of the label
                if (item.getColour().toLowerCase().equals("orange"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.orange));
                else if (item.getColour().toLowerCase().equals("red"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.red));
                else if (item.getColour().toLowerCase().equals("cyan"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.cyan));
                else if (item.getColour().toLowerCase().equals("green"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.green));
                else if (item.getColour().toLowerCase().equals("yellow"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.yellow));
                else if (item.getColour().toLowerCase().equals("purple"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.purple));
                else if (item.getColour().toLowerCase().equals("pink"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.pink));
                else if (item.getColour().toLowerCase().equals("grey"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.grey));
                else if (item.getColour().toLowerCase().equals("brown"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.brown));
                else if (item.getColour().toLowerCase().equals("blue"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.blue));
                else
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.white));

                // Colour of the perimeter of the label
                if (item.getCompleted() == 2) {
                    ((GradientDrawable) background).setStroke(3, getResources().getColor(R.color.dark_green));
                    label.setTextColor(getResources().getColor(R.color.dark_green));
                } else if (item.getCompleted() == 1) {
                    ((GradientDrawable) background).setStroke(3, getResources().getColor(R.color.dark_red));
                    label.setTextColor(getResources().getColor(R.color.dark_red));
                } else
                    ((GradientDrawable) background).setStroke(2, getResources().getColor(R.color.white));
            }


            //Make the label clickable
            label.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getBackground().setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.DARKEN);

                    //Save the current position
                    //MainActivity.pos = index + 1;

                    //Bundle that will be passed to the next activity
                    Bundle bundle = new Bundle();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

                    bundle.putInt("id", item.getId());
                    bundle.putString("date", sdf.format(item.getStartDate()));
                    if (item.getRepeat_interval().equals("NEVER"))
                        bundle.putInt("repeats", 0); //0 means it isn't a repeating task
                    else
                        bundle.putInt("repeats", item.getEnds_after());

                    //Start the new activity
                    Intent intent = new Intent(getActivity(), ViewItemActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });

            //Set the layout of the label
            label.setLayoutParams(new TableRow.LayoutParams(width / 7, TableLayout.LayoutParams.WRAP_CONTENT));
            label.setPadding(8, 0, 8, 5);
            label.setMaxLines(4);
            label.setEllipsize(TextUtils.TruncateAt.END);


            //Add the label to the row
            dayLayout.addView(label);
        }
        row.addView(dayLayout);
    }

    public void addLabelToRow(TableRow row, final ArrayList<PlannerItem> day, final int count, boolean addTime){
        //The views that contain the text
        TextView label = new TextView(getContext());

        //Add data to the label
        if (day.size() > count) {
            //Set the text of the label
            if (addTime) {
                //Formate the time as a string
                String timeString = getTimeString(day.get(count).getStartDate()) + " - " + getTimeString(day.get(count).getEndDate()) + "\n";

                SpannableString text = new SpannableString(timeString + day.get(count).getTitle());
                StyleSpan boldSpan = new StyleSpan(Typeface.ITALIC);
                text.setSpan(boldSpan, 0, 13, 0);
                label.setText(text);
            }
            else {
                label.setText(day.get(count).getTitle());
            }
            label.setTextColor(0xFF000000);

            //Set the colours of the label
            label.setBackground(getResources().getDrawable(R.drawable.item_back));
            Drawable background = label.getBackground();
            if (background instanceof GradientDrawable) {
                // Color of the background of the label
                if (day.get(count).getColour().toLowerCase().equals("orange"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.orange));
                else if (day.get(count).getColour().toLowerCase().equals("red"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.red));
                else if (day.get(count).getColour().toLowerCase().equals("cyan"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.cyan));
                else if (day.get(count).getColour().toLowerCase().equals("green"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.green));
                else if (day.get(count).getColour().toLowerCase().equals("yellow"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.yellow));
                else if (day.get(count).getColour().toLowerCase().equals("purple"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.purple));
                else if (day.get(count).getColour().toLowerCase().equals("pink"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.pink));
                else if (day.get(count).getColour().toLowerCase().equals("grey"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.grey));
                else if (day.get(count).getColour().toLowerCase().equals("brown"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.brown));
                else if (day.get(count).getColour().toLowerCase().equals("blue"))
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.blue));
                else
                    ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), R.color.white));

                // Colour of the perimeter of the label
                if (day.get(count).getCompleted() == 2){
                    ((GradientDrawable) background).setStroke(3, getResources().getColor(R.color.dark_green));
                    label.setTextColor(getResources().getColor(R.color.dark_green));
                }else if (day.get(count).getCompleted() == 1) {
                    ((GradientDrawable) background).setStroke(3, getResources().getColor(R.color.dark_red));
                    label.setTextColor(getResources().getColor(R.color.dark_red));
                }else
                    ((GradientDrawable)background).setStroke(2, getResources().getColor(R.color.white));
            }


            //Make the label clickable
            label.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getBackground().setColorFilter(Color.argb(50,0,0,0), PorterDuff.Mode.DARKEN);

                    //Save the current position
                    MainActivity.pos = index+1;

                    //Bundle that will be passed to the next activity
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", day.get(count).getId());

                    //Start the new activity
                    Intent intent = new Intent(getActivity(), ViewItemActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
        }
        //Set the layout of the label
        label.setLayoutParams(new TableRow.LayoutParams(width / 7, TableLayout.LayoutParams.WRAP_CONTENT));
        label.setPadding(8,0,8,5);
        label.setMaxLines(4);
        label.setEllipsize(TextUtils.TruncateAt.END);


        //Add the label to the row
        row.addView(label);
    }

    public class AddItemClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //MainActivity.pos = index+1;
            Intent intent = new Intent(getActivity(), ItemActivity.class);
            startActivity(intent);
        }
    }


    //Format the time components as a string
    public String getTimeString(Date date){
        String returnString = "";
        if (date.getHours() < 10)
            returnString += "0" + date.getHours() + ":";
        else
            returnString += date.getHours() + ":";

        if (date.getMinutes() < 10)
            returnString += "0" + date.getMinutes();
        else
            returnString += date.getMinutes();

        return returnString;
    }

}
