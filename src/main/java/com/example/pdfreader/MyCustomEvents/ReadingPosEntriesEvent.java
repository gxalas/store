package com.example.pdfreader.MyCustomEvents;

import java.util.EventObject;

public class ReadingPosEntriesEvent extends EventObject {

    /**
     * Constructs a prototypical Event.
     *
     * This Event fires when
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public ReadingPosEntriesEvent(Object source) {
        super(source);
    }
}
