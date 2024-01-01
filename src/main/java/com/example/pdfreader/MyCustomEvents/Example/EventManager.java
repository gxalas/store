package com.example.pdfreader.MyCustomEvents.Example;

import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static EventManager instance;
    private final CopyOnWriteArrayList<CustomEventListener> listeners = new CopyOnWriteArrayList<>();

    private EventManager() {}

    public static EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }
    public void addEventListener(CustomEventListener listener) {
        listeners.addIfAbsent(listener);
    }
    public void removeEventListener(CustomEventListener listener) {
        listeners.remove(listener);
    }
    public void fireStarting(CustomEvent event) {
        for (CustomEventListener listener : listeners) {
            listener.onStarting(event);
        }
    }
    public void fireEnding(CustomEvent event) {
        System.out.println("the ending got fired : ____+____________________________");
        for (CustomEventListener listener : listeners) {
            listener.onEnding(event);
        }
    }
}