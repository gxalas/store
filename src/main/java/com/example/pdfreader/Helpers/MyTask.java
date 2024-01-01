package com.example.pdfreader.Helpers;

import com.example.pdfreader.HelloApplication;
import com.example.pdfreader.HelloController;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;

import java.util.concurrent.Callable;

public class MyTask extends Task<Void> {
    private Callable<Void> taskLogic;
    private final StringProperty myTitle = new SimpleStringProperty();
    private final StringProperty myDescription = new SimpleStringProperty();
    private final ObjectProperty<MyTaskState> myState = new SimpleObjectProperty<>();
    public MyTask(Callable<Void> taskLogic) {
        this.taskLogic = taskLogic;
        this.setMyState(MyTaskState.PENDING);

        this.myState.addListener(new ChangeListener<MyTaskState>() {
            @Override
            public void changed(ObservableValue<? extends MyTaskState> observableValue, MyTaskState myTaskState, MyTaskState t1) {
                System.out.println("the state changed to "+stateProperty().get()+" for "+myDescription.get());
            }
        });










        // Set the onSucceeded event handler within the constructor
        this.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                onSucceededCommon();
            }
        });
        this.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                // Common onSucceeded logic
                onFailedCommon();
            }
        });
    }
    @Override
    protected Void call() throws Exception {
        if (taskLogic != null) {
            return taskLogic.call(); // Execute the provided task logic
        }
        return null;
    }

    public String getMyTitle() {
        return myTitle.get();
    }
    public StringProperty getTitleProperty(){
        return myTitle;
    }

    public void setMyTitle(String myTitle) {
        this.myTitle.set(myTitle);
    }
    public String getMyDescription(){
        return this.myDescription.get();
    }
    public StringProperty getMyDescriptionProperty(){
        return this.myDescription;
    }
    public void setMyDescription(String description){
        this.myDescription.set(description);
    }
    public void setAsRunning(){
        this.myState.set(MyTaskState.RUNNING);
    }
    public MyTaskState getMyState(){
        return this.myState.get();
    }
    public ObjectProperty<MyTaskState> getMyStateProperty(){
        return this.myState;
    }
    public void setMyState(MyTaskState state){
        this.myState.set(state);
    }
    private void onSucceededCommon() {
        // Common code to run on every onSucceeded
        this.setMyState(MyTaskState.COMPLETED);
        //System.out.println("Common Task succeeded logic");
    }
    private void onFailedCommon(){
        this.setMyState(MyTaskState.FAILED);
    }
    public Callable<Void> getTaskLogic(){
        return this.taskLogic;
    }

}
