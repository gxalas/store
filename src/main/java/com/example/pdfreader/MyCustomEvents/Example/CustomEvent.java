package com.example.pdfreader.MyCustomEvents.Example;

public class CustomEvent {
    private final String message;
    private long startTime;
    private long endTime;
    public CustomEvent(String message) {
        this.message = message;
        startTime = System.nanoTime();
    }
    public String getMessage() {
        return message;
    }
    public long getStartTime() {
        return startTime;
    }
    public long getEndTime() {
        return endTime;
    }
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}