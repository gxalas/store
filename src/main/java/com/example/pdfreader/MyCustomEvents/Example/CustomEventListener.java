package com.example.pdfreader.MyCustomEvents.Example;

public interface CustomEventListener {
    default void onStarting(CustomEvent event){};
    default void onEnding(CustomEvent event){};
}
