package com.example.trafficeventsapp;

import android.widget.ImageButton;
import android.widget.ImageView;

public class EventModel {
    private String hist_key;
    private int imageView;
    private String eventType;
    private String eventDate;
    private String eventConfirmationCount;
    private double latitude;
    private double longlatitude;


    public EventModel(int imageView, String eventType, String eventDate, String eventConfirmationCount, double latitude, double longlatitude,String hist_key) {
        this.imageView = imageView;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.eventConfirmationCount = eventConfirmationCount;
        this.latitude = latitude;
        this.longlatitude = longlatitude;
        this.hist_key = hist_key;
    }

    public int getImageView() {
        return imageView;
    }

    public void setImageView(int imageView) {
        this.imageView = imageView;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventConfirmationCount() {
        return eventConfirmationCount;
    }

    public void setEventConfirmationCount(String eventConfirmationCount) {
        this.eventConfirmationCount = eventConfirmationCount;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLonglatitude() {
        return longlatitude;
    }

    public void setLonglatitude(double longlatitude) {
        this.longlatitude = longlatitude;
    }

    public String getHist_key() {
        return hist_key;
    }

    public void setHist_key(String hist_key) {
        this.hist_key = hist_key;
    }
}