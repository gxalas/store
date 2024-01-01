package com.example.pdfreader.MyCustomEvents;

import com.example.pdfreader.HelloController;

import java.util.EventObject;

public class TracingFolderEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public TracingFolderEvent(Object source) {
        super(source);
        ((HelloController)source).listManager.getFailed().clear();
        ((HelloController)source).listManager.getToImportQueue().clear();
    }
}
