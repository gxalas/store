package com.example.pdfreader.MyCustomEvents.DBError;

import java.util.EventListener;

public interface DBErrorListener extends EventListener {
    void errorLogged(DBErrorEvent event);
}
