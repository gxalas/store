package com.example.pdfreader.Helpers;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public class MyThreads extends Thread{

private Task<Void> task;

    public MyThreads(Task<Void> task) {
        this.task = task;

    }

    @Override
    public void run() {
        if (task != null) {
            task.run();
        }
    }



    public Task<Void> getTask() {
        return this.task;
    }
    // Inner class for MyTask extending Task<Void>

}
