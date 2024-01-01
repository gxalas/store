package com.example.pdfreader.MyCustomEvents;

import java.util.EventObject;

public class DocumentsImportedEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     * this event fires when the import of documents
     * that where on the queue has finished
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public DocumentsImportedEvent(Object source) {
        super(source);
    }
}
