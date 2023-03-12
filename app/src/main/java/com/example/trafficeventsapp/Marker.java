package com.example.trafficeventsapp;

public class Marker {

    String eventID;
    String creator;
    String creatorName;
    long creationTime;
    long expirationTime;
    int refreshCount;

    public Marker() {
    }

    public Marker(String eventID, String creator, long creationTime, long expirationTime, int refreshCount) {
        this.eventID = eventID;
        this.creator = creator;
        this.creationTime = creationTime;
        this.expirationTime = expirationTime;
        this.refreshCount = refreshCount;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public void setRefreshCount(int refreshCount) {
        this.refreshCount = refreshCount;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
}
