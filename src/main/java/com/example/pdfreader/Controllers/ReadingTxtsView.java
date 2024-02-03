package com.example.pdfreader.Controllers;

import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;

public class ReadingTxtsView extends ChildController{
    public TableView<Product> table;
    public TabPane tabs;
    boolean change = false;

    @Override
    public void initialize(HelloController hc) {
        parentDelegate = hc;
        tabs.getSelectionModel().select(0);
        parentDelegate.sb.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                System.out.println("txt has been changed");

                try {
                    System.out.println("\n\n\n\ntrying\n\n\n\n");
                    change = true;
                    tabs.getSelectionModel().select(1);
                    tabs.requestLayout();
                    table.requestLayout();
                    table.refresh();
                } catch (Exception e){
                    System.out.println("\n\n\n\nsomething went wrong\n\n\n\n");
                }

            }
        });
        tabs.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(change){
                    System.out.println("the change triggered");
                    tabs.getSelectionModel().select(0);
                    change=false;
                }
            }
        });

    }

    @Override
    public void addMyListeners() {

    }

    @Override
    public void removeListeners(HelloController hc) {

    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return null;
    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }
}
