package com.example.pdfreader.MyCustomEvents;

import java.util.EventListener;

public interface DocumentsImportedListener extends EventListener {
    void documentsImported(DocumentsImportedEvent evt);
}
