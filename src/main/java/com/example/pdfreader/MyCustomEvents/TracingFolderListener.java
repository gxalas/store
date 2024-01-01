package com.example.pdfreader.MyCustomEvents;

import java.util.EventListener;

public interface TracingFolderListener extends EventListener {
    void tracingFolderStarts(TracingFolderEvent evt);
    void tracingFolderEnds(TracingFolderEvent evt);
}
