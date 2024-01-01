package com.example.pdfreader.Controllers;

import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;

public abstract class ChildController{
    protected HelloController parentDelegate;
    public abstract void initialize(HelloController hc);

    /**
     * I think here we add the listeners to
     * either main controller's (hello controller) stuff
     * or to list manager's stuff
     * It is important to be called after initializing
     * so the parent controller has already been set
     */
    public abstract void addMyListeners();
    public abstract void removeListeners(HelloController hc);
    public abstract <T extends ChildController> T getControllerObject();
    public abstract void setState();
    public abstract void getPreviousState();
}
