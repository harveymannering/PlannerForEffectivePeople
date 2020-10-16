package com.businesscompany.harvey.plannerforeffectivepeople;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlannerDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Planner";
    private static final int DB_VERSION = 7;

    PlannerDatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        updateMyDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateMyDatabase(db, oldVersion, newVersion);
    }

    public void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1){
            db.execSQL("CREATE TABLE ITEM " +
                    "(_id INTEGER PRIMARY KEY, " +
                    "TITLE TEXT, " +
                    "DESCRIPTION TEXT, " +
                    "PRIORITY INT, " +
                    "START_DATE DATETIME, " +
                    "END_DATE DATETIME, " +
                    "COLOUR TEXT, " +
                    "COMPLETED INTEGER)");
        }

        if (oldVersion < 2){
            db.execSQL("CREATE TABLE SETTINGS " +
                    "(NOTIFICATIONS_ENABLED INTEGER, " +
                    "START_DAY INTEGER)");

            ContentValues settings = new ContentValues();

            //Add parameters
            settings.put("NOTIFICATIONS_ENABLED", 0);
            settings.put("START_DAY", 0);

            //Run the SQL
            db.insert("SETTINGS", null, settings);
        }

        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE SETTINGS ADD COLUMN NOTIFICATION_MINUTES INTEGER DEFAULT 0");
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE SETTINGS ADD COLUMN DISPLAY_DATES INT DEFAULT 0");
        }

        if (oldVersion < 5){
            db.execSQL("ALTER TABLE ITEM ADD COLUMN REPEAT_INTERVAL TEXT DEFAULT 'NEVER'");
            db.execSQL("ALTER TABLE ITEM ADD COLUMN REPEAT_INTERVAL_NUMBER INT");
            db.execSQL("ALTER TABLE ITEM ADD COLUMN ENDS_AFTER INT");
            db.execSQL("ALTER TABLE ITEM ADD COLUMN MONTHLY_REPEAT_MODE");
            db.execSQL("ALTER TABLE ITEM ADD COLUMN WEEKDAYS INT");
        }

        if (oldVersion < 6){
            db.execSQL("CREATE TABLE REPEATED_ITEMS " +
                    "(_id INTEGER PRIMARY KEY, " +
                    "ITEM_ID INT, " +
                    "COMPLETED INT, " +
                    "REPETITION_NUMBER INT)");
        }

        if (oldVersion < 7){
            db.execSQL("CREATE TABLE ACTIVE_NOTIFICATIONS " +
                    "(_id INTEGER PRIMARY KEY, " +
                    "NOTIFICATION_ID INT)");
        }

    }

    public void addActiveNotifications(SQLiteDatabase db, ArrayList<Integer> notificationIds) {
        for (Integer id : notificationIds){
            //Add parameters
            ContentValues itemValues = new ContentValues();
            itemValues.put("NOTIFICATION_ID", id);

            //Run the SQL
            db.insert("ACTIVE_NOTIFICATIONS", null, itemValues);
        }
    }

    public void deleteActiveNotification(SQLiteDatabase db, Integer notificationId) {
        db.delete("ACTIVE_NOTIFICATIONS", "NOTIFICATION_ID = " + notificationId, null);
    }

    public ArrayList<Integer> getActiveNotifications(SQLiteDatabase db) {
        ArrayList<Integer> output = new ArrayList<Integer>();

        try{
            //Define cursor for accessing data
            Cursor cursor = db.rawQuery("SELECT NOTIFICATION_ID FROM ACTIVE_NOTIFICATIONS", null);

            //Move cursor to first record
            if (cursor.moveToFirst()){
                output.add(cursor.getInt(0));

                //Loop until all records have been processed
                while (cursor.moveToNext()) {
                    output.add(cursor.getInt(0));
                }
            }

            //Close database resources
            cursor.close();

        } catch (Exception e) {

        }

        return output;
    }

    public boolean isNotificationEnabled(SQLiteDatabase db){
        //Query the database
        Cursor cursor = db.rawQuery("SELECT NOTIFICATIONS_ENABLED FROM SETTINGS", null);

        //Look at results of query
        if (cursor.moveToFirst()){
            int enabled = cursor.getInt(0);
            if (enabled == 1)
                return true;
            else
                return false;
        }

        return false;
    }

    public boolean isSettingDisplayed(SQLiteDatabase db, String setting_name){
        //Query the database
        Cursor cursor = db.rawQuery("SELECT " + setting_name +" FROM SETTINGS", null);

        //Look at results of query
        if (cursor.moveToFirst()){
            int enabled = cursor.getInt(0);
            if (enabled == 1)
                return true;
            else
                return false;
        }

        return false;
    }

    public int getStartDay(SQLiteDatabase db){
        //Query the database
        Cursor cursor = db.rawQuery("SELECT START_DAY FROM SETTINGS", null);

        //Look at results of query
        if (cursor.moveToFirst())
            return cursor.getInt(0);

        return 0;
    }

    public int getNotificationMinutes(SQLiteDatabase db){
        //Query the database
        Cursor cursor = db.rawQuery("SELECT NOTIFICATION_MINUTES FROM SETTINGS", null);

        //Look at results of query
        if (cursor.moveToFirst())
            return cursor.getInt(0);

        return 0;
    }

    public void updateSettings(SQLiteDatabase db, boolean notificationsEnabled, int day, int notificationMinutes, boolean displayDates){

        //Convert enabled booleans into an ints
        int notifications_enabled_int = 0;
        int display_dates_int = 0;
        if (notificationsEnabled)
            notifications_enabled_int = 1;
        if (displayDates)
            display_dates_int = 1;

        //Define input parameters
        ContentValues settings = new ContentValues();
        settings.put("NOTIFICATIONS_ENABLED", notifications_enabled_int);
        settings.put("START_DAY", day);
        settings.put("NOTIFICATION_MINUTES", notificationMinutes);
        settings.put("DISPLAY_DATES", display_dates_int);

        //Update database
        db.update("SETTINGS", settings, null, null);
    }

    //Adds an item to the ITEM table, this represents one activity on the planner screen
    public void insertItem(SQLiteDatabase db, PlannerItem item){
        //Useful objects
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        ContentValues itemValues = new ContentValues();

        //Add parameters
        itemValues.put("TITLE", item.getTitle());
        itemValues.put("DESCRIPTION", item.getDescription());
        itemValues.put("PRIORITY", item.getPriority());
        itemValues.put("START_DATE", dateFormat.format(item.getStartDate()));
        itemValues.put("END_DATE", dateFormat.format(item.getEndDate()));
        itemValues.put("COLOUR", item.getColour());
        itemValues.put("COMPLETED", 0);
        itemValues.put("REPEAT_INTERVAL", item.getRepeat_interval());
        itemValues.put("REPEAT_INTERVAL_NUMBER", item.getRepeat_interval_number());
        itemValues.put("ENDS_AFTER", item.getEnds_after());
        itemValues.put("MONTHLY_REPEAT_MODE", item.getMonthly_repeat_mode());
        itemValues.put("WEEKDAYS", item.getWeekdays());

        //Run the SQL
        db.insert("ITEM", null, itemValues);
    }

    //Updates an item to the ITEM table
    public void updateItem(SQLiteDatabase db, PlannerItem item){
        //Useful objects
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        ContentValues itemValues = new ContentValues();

        //Add parameters
        itemValues.put("TITLE", item.getTitle());
        itemValues.put("DESCRIPTION", item.getDescription());
        itemValues.put("PRIORITY", item.getPriority());
        itemValues.put("START_DATE", dateFormat.format(item.getStartDate()));
        itemValues.put("END_DATE", dateFormat.format(item.getEndDate()));
        itemValues.put("COLOUR", item.getColour());
        itemValues.put("REPEAT_INTERVAL", item.getRepeat_interval());
        itemValues.put("REPEAT_INTERVAL_NUMBER", item.getRepeat_interval_number());
        itemValues.put("ENDS_AFTER", item.getEnds_after());
        itemValues.put("MONTHLY_REPEAT_MODE", item.getMonthly_repeat_mode());
        itemValues.put("WEEKDAYS", item.getWeekdays());
        db.update("ITEM", itemValues, "_id="+item.getId(), null);

        db.delete("REPEATED_ITEMS", "ITEM_ID = " + item.getId(), null);
    }

    //When completed param is:
    //  0 it is unset
    //  1 it is failed
    //  2 it is completed
    public void updateCompletedStatus(SQLiteDatabase db, int id, int completed, int repeats){
        //Useful objects
        ContentValues itemValues = new ContentValues();

        //Add parameters
        if (repeats == 0) {
            itemValues.put("COMPLETED", completed);
            db.update("ITEM", itemValues, "_id=" + id, null);
        }
        else {
            try {
                //Define cursor for accessing data
                Cursor cursor = db.query("REPEATED_ITEMS",
                        new String[]{
                                "_id",
                                "ITEM_ID",
                                "REPETITION_NUMBER"
                        },
                        "ITEM_ID = " + id + " AND REPETITION_NUMBER = " + repeats, null, null, null, null);

                //Move cursor to first record
                if (cursor.moveToFirst()) {
                    itemValues.put("COMPLETED", completed);
                    db.update("REPEATED_ITEMS", itemValues, "_id=" + cursor.getInt(0), null);
                }
                else {
                    //Add parameters
                    itemValues.put("ITEM_ID", id);
                    itemValues.put("COMPLETED", completed);
                    itemValues.put("REPETITION_NUMBER", repeats);

                    //Run the SQL
                    db.insert("REPEATED_ITEMS", null, itemValues);
                }

                //Close database resources
                cursor.close();
            } catch (Exception e){

            }
        }
    }

    public int getRepeatedCompleteStatus(SQLiteDatabase db, int id, int repeats){
        //Useful objects
        ContentValues itemValues = new ContentValues();
        int returnValue = 0;
        try {
            //Define cursor for accessing data
            Cursor cursor = db.query("REPEATED_ITEMS",
                    new String[]{
                            "_id",
                            "ITEM_ID",
                            "COMPLETED",
                            "REPETITION_NUMBER"
                    },
                    "ITEM_ID = " + id + " AND REPETITION_NUMBER = " + repeats, null, null, null, null);

            //Move cursor to first record
            if (cursor.moveToFirst()) {
                returnValue = cursor.getInt(2);
            }

            //Close database resources
            cursor.close();
        } catch (Exception e){

        }

        return returnValue;
    }

    public List<PlannerItem> getItems(SQLiteDatabase db, int priorities, Date startDate, Date endDate){
        List<PlannerItem> returnList = new ArrayList<PlannerItem>(); //List to be returned
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //Used to read dates from the database
        int i = 0; //Index

        try{
            //Define cursor for accessing data
            Cursor cursor = db.query("ITEM",
                    new String[] {
                            "_id",
                            "TITLE",
                            "DESCRIPTION",
                            "PRIORITY",
                            "START_DATE",
                            "END_DATE",
                            "COLOUR",
                            "COMPLETED",
                            "REPEAT_INTERVAL",
                            "REPEAT_INTERVAL_NUMBER",
                            "ENDS_AFTER",
                            "MONTHLY_REPEAT_MODE",
                            "WEEKDAYS"
                    },
                    "(PRIORITY = " + priorities + " ) AND ((START_DATE >= '" + dateFormat.format(startDate) + "' AND END_DATE <= '" + dateFormat.format(endDate) + "') OR REPEAT_INTERVAL != 'NEVER')",
                    null, null, null, "START_DATE ASC");

            //Move cursor to first record
            if (cursor.moveToFirst()){
                returnList.add(new PlannerItem(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        dateFormat.parse(cursor.getString(4)),
                        dateFormat.parse(cursor.getString(5)),
                        cursor.getString(6),
                        cursor.getInt(7),
                        cursor.getString(8),
                        cursor.getInt(9),
                        cursor.getInt(10),
                        cursor.getInt(11),
                        cursor.getInt(12)
                ));

                //Loop until all records have been processed
                while (cursor.moveToNext()) {
                    i += 1;
                    returnList.add(new PlannerItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getInt(3),
                            dateFormat.parse(cursor.getString(4)),
                            dateFormat.parse(cursor.getString(5)),
                            cursor.getString(6),
                            cursor.getInt(7),
                            cursor.getString(8),
                            cursor.getInt(9),
                            cursor.getInt(10),
                            cursor.getInt(11),
                            cursor.getInt(12)
                    ));
                }
            }

            //Close database resources
            cursor.close();

        } catch (Exception e) {
            returnList.clear();
            returnList.add(new PlannerItem(
                    0,
                    "ERROR",
                    "An error occured while accessing the database",
                    0,
                    new Date(),
                    new Date(),
                    "RED",
                    0,
                    "NEVER",
                    1,
                    0,
                    0,
                    0

            ));
        }

        returnList = getRepeatedItems(db, returnList, startDate, endDate);

        return returnList;
    }

    private List<PlannerItem> getRepeatedItems(SQLiteDatabase db, List<PlannerItem> inputList, Date startDate, Date endDate){

        List<PlannerItem> outputList = new ArrayList<PlannerItem>();

        for (PlannerItem item: inputList){
            //If item does not repeat it can be added to output
            if (item.getRepeat_interval().equals("NEVER")) {
                outputList.add(item);
            }
            //Calculations must be done on an items that repeats daily to determine what must be added to output
            else if (item.getRepeat_interval().equals("DAILY") || item.getRepeat_interval().equals("YEARLY")){
                Calendar c_start = Calendar.getInstance();
                c_start.setTime(item.getStartDate());
                Calendar c_end = Calendar.getInstance();
                c_end.setTime(item.getEndDate());
                int numOfRepeats = item.getEnds_after();
                boolean finite = false;
                if (numOfRepeats > 0)
                    finite = true;

                //if the time is 00:00, the while condition gets fucked, so just add a second to the time for convinence
                if (c_start.get(Calendar.HOUR_OF_DAY) == 0 && c_start.get(Calendar.MINUTE) == 0){
                    c_start.add(Calendar.SECOND, 1);
                }

                //iterate through all of the items that will be generated through repetition
                while (c_start.getTime().before(endDate) && (finite == false || (finite == true && numOfRepeats > 0))){
                    if (c_start.getTime().after(startDate)  && c_start.getTime().before(endDate))
                        outputList.add(new PlannerItem(
                            item.getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getPriority(),
                            c_start.getTime(),
                            c_end.getTime(),
                            item.getColour(),
                            getRepeatedCompleteStatus(db, item.getId(), (item.getEnds_after() + 1) - numOfRepeats),
                            item.getRepeat_interval(),
                            item.getRepeat_interval_number(),
                            (item.getEnds_after() + 1) - numOfRepeats,
                            item.getMonthly_repeat_mode(),
                            item.getWeekdays()
                        ));

                    //Add a number of days/years
                    if (item.getRepeat_interval().equals("DAILY")){
                        c_start.add(Calendar.DATE, item.getRepeat_interval_number());
                        c_end.add(Calendar.DATE, item.getRepeat_interval_number());
                    }
                    else if (item.getRepeat_interval().equals("YEARLY")){
                        c_start.add(Calendar.YEAR, item.getRepeat_interval_number());
                        c_end.add(Calendar.YEAR, item.getRepeat_interval_number());
                    }


                    numOfRepeats--;
                }
            }
            //For a task that repeats weekly we also need to take into account that the user may have specified which days the task should be on
            else if (item.getRepeat_interval().equals("WEEKLY")){
                //set up loop variables
                Calendar c_start = Calendar.getInstance();
                c_start.setTime(item.getStartDate());
                Calendar c_end = Calendar.getInstance();
                c_end.setTime(item.getEndDate());
                int numOfRepeats = item.getEnds_after();
                boolean finite = false;
                if (numOfRepeats > 0)
                    finite = true;

                //Which days of the week have been selected?
                boolean mon = false, tue = false, wed = false, thu = false, fri = false, sat = false, sun = false;
                if ((1 & item.getWeekdays()) == 1)
                    mon = true;
                if ((2 & item.getWeekdays()) == 2)
                    tue = true;
                if ((4 & item.getWeekdays()) == 4)
                    wed = true;
                if ((8 & item.getWeekdays()) == 8)
                    thu = true;
                if ((16 & item.getWeekdays()) == 16)
                    fri = true;
                if ((32 & item.getWeekdays()) == 32)
                    sat = true;
                if ((64 & item.getWeekdays()) == 64)
                    sun = true;

                int count = 0;
                //if the time is 00:00, the while condition gets fucked, so just add a second to the time for convinence
                if (c_start.get(Calendar.HOUR_OF_DAY) == 0 && c_start.get(Calendar.MINUTE) == 0){
                    c_start.add(Calendar.SECOND, 1);
                }
                while (!c_start.getTime().after(endDate) && (finite == false || (finite == true && numOfRepeats > 0))){

                    boolean addDate = false;
                    if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) && (mon)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) && (tue)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) && (wed)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) && (thu)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) && (fri)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) && (sat)) {
                        addDate = true;
                    } else if ((c_start.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) && (sun)) {
                        addDate = true;
                    }

                    if (addDate) {
                        if (c_start.getTime().compareTo(startDate) >= 0 && c_start.getTime().compareTo(endDate) <= 0) {
                            outputList.add(new PlannerItem(
                                    item.getId(),
                                    item.getTitle(),
                                    item.getDescription(),
                                    item.getPriority(),
                                    c_start.getTime(),
                                    c_end.getTime(),
                                    item.getColour(),
                                    getRepeatedCompleteStatus(db, item.getId(), (item.getEnds_after() + 1) - numOfRepeats),
                                    item.getRepeat_interval(),
                                    item.getRepeat_interval_number(),
                                    (item.getEnds_after() + 1) - numOfRepeats,
                                    item.getMonthly_repeat_mode(),
                                    item.getWeekdays()
                            ));
                        }
                        numOfRepeats--;
                    }
                    c_start.add(Calendar.DATE, 1);
                    c_end.add(Calendar.DATE, 1);
                    count++;
                    if (count != 0 && count % 7 == 0){
                        c_start.add(Calendar.DATE, 7 * (item.getRepeat_interval_number() - 1));
                        c_end.add(Calendar.DATE, 7 * (item.getRepeat_interval_number() - 1));
                    }

                }
            }
            //Monthly repeats have two modes - EXAMPLE: 0 = repeat on the 19th of the month, 1 = repeat every 3rd thursday
            else if (item.getRepeat_interval().equals("MONTHLY")){
                Calendar c_start = Calendar.getInstance();
                c_start.setTime(item.getStartDate());
                Calendar c_end = Calendar.getInstance();
                c_end.setTime(item.getEndDate());
                int numOfRepeats = item.getEnds_after();
                boolean finite = false;
                if (numOfRepeats > 0)
                    finite = true;

                //if the time is 00:00, the while condition gets fucked, so just add a second to the time for convinence
                if (c_start.get(Calendar.HOUR_OF_DAY) == 0 && c_start.get(Calendar.MINUTE) == 0){
                    c_start.add(Calendar.SECOND, 1);
                }

                 //Count the instances of that specific day (e.g. how many fridays up to and including the 14th of may?)
                int day_of_week = c_start.get(Calendar.DAY_OF_WEEK);
                int count_original = 0;
                if (item.getMonthly_repeat_mode() == 1) {

                    Calendar c_count = Calendar.getInstance();
                    c_count.setTime(item.getStartDate());
                    c_count.set(Calendar.DAY_OF_MONTH, 1);

                    for (int i = 0; i < c_start.get(Calendar.DAY_OF_MONTH); i++){
                        if (c_count.get(Calendar.DAY_OF_WEEK) == day_of_week)
                            count_original++;
                        c_count.add(Calendar.DATE, 1);
                    }
                }

                //iterate through all of the items that will be generated through repetition
                while (c_start.getTime().before(endDate) && (finite == false || (finite == true && numOfRepeats > 0))) {
                    if (c_start.getTime().after(startDate) && c_start.getTime().before(endDate))
                        outputList.add(new PlannerItem(
                                item.getId(),
                                item.getTitle(),
                                item.getDescription(),
                                item.getPriority(),
                                c_start.getTime(),
                                c_end.getTime(),
                                item.getColour(),
                                getRepeatedCompleteStatus(db, item.getId(), (item.getEnds_after() + 1) - numOfRepeats),
                                item.getRepeat_interval(),
                                item.getRepeat_interval_number(),
                                (item.getEnds_after() + 1) - numOfRepeats,
                                item.getMonthly_repeat_mode(),
                                item.getWeekdays()
                        ));

                    //Add a number of months
                    if (item.getMonthly_repeat_mode() == 0){
                        c_start.add(Calendar.MONTH, item.getRepeat_interval_number());
                        c_end.add(Calendar.MONTH, item.getRepeat_interval_number());
                    }
                    else {
                        //Set calendar to be the first day of the month
                        c_start.add(Calendar.MONTH, item.getRepeat_interval_number());
                        c_end.add(Calendar.MONTH, item.getRepeat_interval_number());
                        c_start.set(c_start.get(Calendar.YEAR), c_start.get(Calendar.MONTH), 1);
                        c_end.set(c_end.get(Calendar.YEAR), c_end.get(Calendar.MONTH), 1);

                        //Find the day of the month in question (e.g. find the third Tuesday of the month)
                        boolean lastDay = false;
                        int month = c_start.get(Calendar.MONTH);
                        int count = count_original;
                        while (count > 0) {
                            if (c_start.get(Calendar.DAY_OF_WEEK) == day_of_week) {
                                count--;
                                if (count <= 0)
                                    break;
                            }
                            if (c_start.get(Calendar.MONTH) > month)
                                lastDay = true;

                            if (lastDay) {
                                c_start.add(Calendar.DATE, -1);
                                c_end.add(Calendar.DAY_OF_MONTH, -1);
                            }
                            else {
                                c_start.add(Calendar.DATE, 1);
                                c_end.add(Calendar.DAY_OF_MONTH, 1);
                            }
                        }
                    }

                    numOfRepeats--;
                }
            }
        }
        return outputList;
    }

    public boolean repetitionExists(SQLiteDatabase db, int id, int repeats){

        boolean result = false;
        if (repeats == 0)
            return false;

        try{
            //Define cursor for accessing data
            Cursor cursor = db.query("ITEM",
                    new String[] {
                            "_id",
                            "REPEAT_INTERVAL",
                            "ENDS_AFTER"
                    },
                    "_id = " + id + " AND REPEAT_INTERVAL != 'NEVER'",
                    null, null, null, "START_DATE ASC");

            //Move cursor to first record
            if (cursor.moveToFirst()){
                int val = cursor.getInt(2);
                if (val >= repeats || val == 0)
                    result = true;
            }



            //Close database resources
            cursor.close();

        } catch (Exception e) {

        }

        return result;
    }

    public static void deleteItem(SQLiteDatabase db, int id) {
        db.delete("ITEM", "_id = " + id, null);
    }

    public PlannerItem getItem(SQLiteDatabase db, int id){
        //Default return value
        PlannerItem returnItem = new PlannerItem(0, "", "", 0, new Date(), new Date(), "", 0, "NEVER", 1, 0, 0, 0);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //Used to read dates from the database

        try{
            //Define cursor for accessing data
            Cursor cursor = db.query("ITEM",
                    new String[] {
                            "_id",
                            "TITLE",
                            "DESCRIPTION",
                            "PRIORITY",
                            "START_DATE",
                            "END_DATE",
                            "COLOUR",
                            "COMPLETED",
                            "REPEAT_INTERVAL",
                            "REPEAT_INTERVAL_NUMBER",
                            "ENDS_AFTER",
                            "MONTHLY_REPEAT_MODE",
                            "WEEKDAYS"
                    },
                    "_id = " + id, null, null, null, null);

            //Move cursor to first record
            if (cursor.moveToFirst()){
                returnItem = new PlannerItem(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        dateFormat.parse(cursor.getString(4)),
                        dateFormat.parse(cursor.getString(5)),
                        cursor.getString(6),
                        cursor.getInt(7),
                        cursor.getString(8),
                        cursor.getInt(9),
                        cursor.getInt(10),
                        cursor.getInt(11),
                        cursor.getInt(12)
                );
            }

            //Close database resources
            cursor.close();

        } catch (Exception e) {
            returnItem = new PlannerItem(
                    0,
                    "ERROR",
                    "An error occured while accessing the database",
                    0,
                    new Date(),
                    new Date(),
                    "RED",
                    0,
                    "NEVER",
                    1,
                    0,
                    0,
                    0
            );
        }

        return returnItem;
    }

    //Gets the oldest date in the database
    public Date getFirstDate(SQLiteDatabase db){
        Date firstDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()); //Used to read dates from the database

        try{
            //Define cursor for accessing data
            Cursor cursor = db.query("ITEM", new String[] {"START_DATE"},
                    null, null, null, null, "START_DATE ASC");

            //Move cursor to first record
            if (cursor.moveToFirst()){
                firstDate = dateFormat.parse(cursor.getString(0));
            }

            //Close database resources
            cursor.close();

        }
        catch (Exception e) {
            firstDate = Calendar.getInstance().getTime();
        }
        return firstDate;
    }

    //Gets the newest date
    public Date getLastDate(SQLiteDatabase db){
        Date lastDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()); //Used to read dates from the database

        try{
            //Define cursor for accessing data
            Cursor cursor = db.query("ITEM", new String[] {"START_DATE"},
                    null, null, null, null, "START_DATE DESC");

            //Move cursor to first record
            if (cursor.moveToFirst()){
                lastDate = dateFormat.parse(cursor.getString(0));
            }

            //Close database resources
            cursor.close();
        }
        catch (Exception e) {
            lastDate = Calendar.getInstance().getTime();
        }
        return lastDate;
    }
}
