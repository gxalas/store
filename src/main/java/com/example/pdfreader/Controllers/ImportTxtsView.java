package com.example.pdfreader.Controllers;

import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;

public class ImportTxtsView extends ChildController{
    @Override
    public void initialize(HelloController hc) {
        System.out.println("importing text is here");
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
