package com.example.pdfreader.MyCustomEvents.DBError;

import com.example.pdfreader.DAOs.DBError;

import java.util.EventObject;

public class DBErrorEvent extends EventObject {
    private final DBError dbError;

    public DBErrorEvent(Object source, DBError dbError) {
        super(source);
        this.dbError = dbError;
    }

    public DBError getDBError() {
        return dbError;
    }
}
