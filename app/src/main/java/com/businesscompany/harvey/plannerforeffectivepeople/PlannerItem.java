package com.businesscompany.harvey.plannerforeffectivepeople;

import java.util.Date;

public class PlannerItem implements Comparable<PlannerItem>{

    //Variables
    private int id = 0;
    private String title;
    private String description;
    private int priority;
    private Date startDate;
    private Date endDate;
    private String colour;
    private int completed;

    private String repeat_interval;
    private int repeat_interval_number;
    private int ends_after;
    private int monthly_repeat_mode;
    private int weekdays;

    public PlannerItem(int id, String title,
                       String description,
                       int priority,
                       Date startDate,
                       Date endDate,
                       String colour,
                       int completed,
                       String repeat_interval,
                       int repeat_interval_number,
                       int ends_after,
                       int monthly_repeat_mode,
                       int weekdays)
    {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.colour = colour;
        this.completed = completed;

        this.repeat_interval = repeat_interval;
        this.repeat_interval_number = repeat_interval_number;
        this.ends_after = ends_after;
        this.monthly_repeat_mode = monthly_repeat_mode;
        this.weekdays = weekdays;
    }

    public PlannerItem(int id,
                       String title,
                       String description,
                       boolean priority,
                       Date startDate,
                       Date endDate,
                       String colour,
                       int completed,
                       String repeat_interval,
                       int repeat_interval_number,
                       int ends_after,
                       int monthly_repeat_mode,
                       int weekdays)
    {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.colour = colour;
        this.completed = completed;

        if (priority)
            this.priority = 1;
        else
            this.priority = 0;

        this.repeat_interval = repeat_interval;
        this.repeat_interval_number = repeat_interval_number;
        this.ends_after = ends_after;
        this.monthly_repeat_mode = monthly_repeat_mode;
        this.weekdays = weekdays;
    }

    //Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        if (priority)
            this.priority = 1;
        else
            this.priority = 0;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        if (completed) 
            this.completed = 1;
        else
            this.completed = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRepeat_interval() {
        return repeat_interval;
    }

    public void setRepeat_interval(String repeat_interval) {
        this.repeat_interval = repeat_interval;
    }

    public int getRepeat_interval_number() {
        return repeat_interval_number;
    }

    public void setRepeat_interval_number(int repeat_interval_number) {
        this.repeat_interval_number = repeat_interval_number;
    }

    public int getEnds_after() {
        return ends_after;
    }

    public void setEnds_after(int ends_after) {
        this.ends_after = ends_after;
    }

    public int getMonthly_repeat_mode() {
        return monthly_repeat_mode;
    }

    public void setMonthly_repeat_mode(int monthly_repeat_mode) {
        this.monthly_repeat_mode = monthly_repeat_mode;
    }

    public int getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(int weekdays) {
        this.weekdays = weekdays;
    }

    @Override
    public int compareTo(PlannerItem o) {
        return startDate.compareTo(o.getStartDate());
    }
}
