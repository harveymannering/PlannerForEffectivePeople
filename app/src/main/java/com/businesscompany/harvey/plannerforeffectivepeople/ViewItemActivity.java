package com.businesscompany.harvey.plannerforeffectivepeople;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

public class ViewItemActivity extends AppCompatActivity {

    //Database
    PlannerDatabaseHelper plannerDatabaseHelper;

    //Variables
    private int id;
    private String date;
    private boolean completed = false;
    private boolean failed = false;
    private String title;
    private String description;
    private int repeats;
    PlannerItem plannerItem;

    //Views
    View cancel_button;
    View confirm_button;

    public static final String ANDROID_CHANNEL_ID = "com.businesscompany.harvey.plannerforeffectivepeople";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_item);

        //Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_view_item);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("");

        //Define datebase
        plannerDatabaseHelper = new PlannerDatabaseHelper(this);

        //Information about the planner item
        int complete;
        try {
            Intent in = getIntent();
            Bundle b = in.getExtras();
            id = b.getInt("id");
            date = b.getString("date");
            repeats = b.getInt("repeats");
        }
        catch (Exception e) {
            id = 0;
            date = "2020/01/01";
        }
    }

    public void onStart() {
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            plannerItem = plannerDatabaseHelper.getItem(db, id);
            int complete_int;
            if (!plannerItem.getRepeat_interval().equals("NEVER"))
                complete_int = plannerDatabaseHelper.getRepeatedCompleteStatus(db, id, repeats);
            else
                complete_int = plannerItem.getCompleted();

            db.close();

            //Set the completed/failed flags
            if (complete_int == 2)
                completed = true;
            else if (complete_int == 1)
                failed = true;

            //If the task is not found in the database
            if (plannerItem.getId() == 0) {
                Toast toast = Toast.makeText(this, "Task unavailable", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }

            //Set the information in the views
            TextView txtTitle = (TextView) findViewById(R.id.vTitle);
            TextView txtDescription = (TextView) findViewById(R.id.vDescription);
            TextView txtDate = (TextView) findViewById(R.id.vDate);
            txtTitle.setText(plannerItem.getTitle());
            txtDescription.setText(plannerItem.getDescription());

            //Set the information in the date string
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
            if (date == null)
                date = sdf1.format(plannerItem.getStartDate());

            if (plannerItem.getPriority() == 1){
                txtDate.setText(date);
            }
            else {

                SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm");
                txtDate.setText(date + " from " +
                                sdf2.format(plannerItem.getStartDate()) + " to " +
                                sdf2.format(plannerItem.getEndDate()));
            }
        } catch (Exception e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        super.onStart();
    }

    //Adds the check and cross icons in the toolbar
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_confirm_cancel, menu);

        //Change the color of the icons in action bar
        Drawable cancel = menu.getItem(0).getIcon();
        cancel.mutate();
        cancel.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        Drawable confirm = menu.getItem(1).getIcon();
        confirm.mutate();
        confirm.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        //Change the color of the toolbar items, this needs its own thread
        new Handler().post(new Runnable() {
           @Override
           public void run() {
               try {
                   refreshToolbarButtons();
               } catch (Exception e) {
                   //Just stops to app from crashing if there's a null pointer
               }
           }
        });

        //Call super class
        return super.onCreateOptionsMenu(menu);
    }

    //Add functionality to the check and cross icons in the toolbar
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_confirm:
                if (failed)
                    failed = false;

                if (completed)
                    completed = false;
                else
                    completed = true;

                updateCompletedStatus();
                refreshToolbarButtons();
                return true;
            case R.id.menu_cancel:
                if (completed)
                    completed = false;

                if (failed)
                    failed = false;
                else
                    failed = true;

                updateCompletedStatus();
                refreshToolbarButtons();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onEditClick(View v){

        //Bundle that will be passed to the next activity
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);

        //Edits flag = tells the next view this is an edit of the current item and not a copy
        bundle.putBoolean("edit_flag", true);

        //Start the new activity
        Intent intent = new Intent(ViewItemActivity.this, ItemActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void onDeleteClick(View v){

        if (!plannerItem.getRepeat_interval().equals("NEVER")) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            deleteNote();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(ViewItemActivity.this);
            builder.setMessage("This will delete all repetitions of this task.  Are you sure you would like to delete this task?");
            builder.setPositiveButton("Yes", dialogClickListener);
            builder.setNegativeButton("No", dialogClickListener);
            builder.show();
        }
        else {
            deleteNote();
        }
    }

    public void deleteNote(){
        //Delete the note in the database
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            if (id > 0) {
                plannerDatabaseHelper.deleteItem(db, id);
            }
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Start the new activity
        finish();
    }

    public void onCopyClick(View v){
        //Bundle that will be passed to the next activity
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);

        //Edits flag = tells the next view this is an edit of the current item and not a copy
        bundle.putBoolean("edit_flag", false);

        //Start the new activity
        Intent intent = new Intent(ViewItemActivity.this, ItemActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    //Change the colours of the completed and failed buttons in the toolbar
    public void refreshToolbarButtons(){

        View confirm_button = (View) findViewById(R.id.menu_confirm);
        if (completed)
            confirm_button.setBackgroundColor(getResources().getColor(R.color.dark_green));
        else
            confirm_button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        View cancel_button = (View) findViewById(R.id.menu_cancel);
        if (failed)
            cancel_button.setBackgroundColor(getResources().getColor(R.color.dark_red));
        else
            cancel_button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    //Update the completed flag in the database
    public void updateCompletedStatus(){
        //Define the input parameter used to update the database
        int completedInteger = 0;
        if (failed)
            completedInteger = 1;
        if (completed)
            completedInteger = 2;

        //Update the database
        try {
            SQLiteDatabase db = plannerDatabaseHelper.getReadableDatabase();
            plannerDatabaseHelper.updateCompletedStatus(db, id, completedInteger, repeats);
            db.close();
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
