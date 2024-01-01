package com.example.pdfreader.MyCustomEvents.DBError;

import com.example.pdfreader.DAOs.DBError;

import java.util.ArrayList;
import java.util.List;

public class ErrorEventManager {
    private List<DBErrorListener> listeners = new ArrayList<>();

    public void addListener(DBErrorListener listener) {
        listeners.add(listener);
    }

    public void notifyErrorLogged(DBError dbError) {
        DBErrorEvent event = new DBErrorEvent(this, dbError);
        for (DBErrorListener listener : listeners) {
            listener.errorLogged(event);
        }
    }
}
