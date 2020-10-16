package com.businesscompany.harvey.plannerforeffectivepeople;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class ParentPlannerFragment extends Fragment {

    static List<Date> d;
    static ViewPager mViewPager;


    PlannerDatabaseHelper plannerDatabaseHelper;

    public ParentPlannerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_parent_planner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Define datebase
        plannerDatabaseHelper = new PlannerDatabaseHelper(getContext());

        //gets first and last dates of the planner
        Date startDate = Calendar.getInstance().getTime();
        Date endDate = Calendar.getInstance().getTime();
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            startDate = plannerDatabaseHelper.getFirstDate(db);
            endDate = plannerDatabaseHelper.getLastDate(db);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(getActivity(), "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Get the date range of the planner
        d = getListOfDates(startDate, endDate);

        //Set up the view pager
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MyAdapter(getChildFragmentManager()));

        //Find the current position in the view pager
        Date currentDate = Calendar.getInstance().getTime();
        if (MainActivity.selectedTime != null)
            currentDate = MainActivity.selectedTime;
        if (MainActivity.pos == -1)

            currentDate = Calendar.getInstance().getTime();


        //Set the position (week) in the view pager
        int offset = 0;
        if (MainActivity.pos == -1)
            offset = -1;

        for (int i = 0; i < d.size(); i++) {
            if (d.get(i).compareTo(currentDate) >= 0) {
                MainActivity.pos = i + offset;
                break;
            }
        }

        mViewPager.setCurrentItem(MainActivity.pos);


    }

    public static class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return d.size() - 1;
        }

        @Override
        public Fragment getItem(int position) {
            return new PlannerFragment(position, d.get(position), d.get(position+1));
        }
    }

    //Gets a list of dates, representing every week that is displayed on the planner
    public List<Date> getListOfDates(Date startDate, Date endDate){
        //Read the day range in the database
        int dayRange = 1; //default monday - sunday

        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            dayRange = plannerDatabaseHelper.getStartDay(db);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(getActivity(), "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //If the end date is within the next three months (or before), the scedule should end at three months from todays date
        Calendar todayPlusThreeMonths = Calendar.getInstance();
        todayPlusThreeMonths.add(Calendar.MONTH, 3);
        if (endDate.before(todayPlusThreeMonths.getTime())) //Calendar.getInstance().getTime() is the current date
            endDate = todayPlusThreeMonths.getTime();


        //If the start date is after 3 months before today, the scedule should start 3 months before todays date
        todayPlusThreeMonths.add(Calendar.MONTH, -6);
        if (startDate.after(todayPlusThreeMonths.getTime()))
            startDate = todayPlusThreeMonths.getTime();

        //Used to just grab the date (times are ignored)
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault());

        //List that represents each week that will be displayed
        List<Date> returnDates = new ArrayList<Date>();

        //Find fist day that will be displayed
        Calendar c_start = Calendar.getInstance();
        c_start.setTime(startDate);
        if (startDate.getDay() < dayRange)
            c_start.add(Calendar.DATE, -7 - (startDate.getDay() - dayRange));
        else if (startDate.getDay() > dayRange)
            c_start.add(Calendar.DATE, -(startDate.getDay() - dayRange));

        //Find the final day that will be displayed
        Calendar c_end = Calendar.getInstance();
        c_end.setTime(endDate);
        if (endDate.getDay() >= dayRange)
            c_end.add(Calendar.DATE, (7 - ((endDate.getDay() - dayRange) - 1)));
        else
            c_end.add(Calendar.DATE, (dayRange - endDate.getDay()) + 1);

        //Get the beginning day of every week in between the start and end dates
        while (c_start.getTime().before(c_end.getTime())){
            //Add a the first day in the week to the list
            try {
                returnDates.add(dateFormat.parse(dateFormat.format(c_start.getTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //Add a week to the calender
            c_start.add(Calendar.DATE, 7);
        }

        //Add a final day on the list
        try {
            returnDates.add(dateFormat.parse(dateFormat.format(c_start.getTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return returnDates;
    }
}
