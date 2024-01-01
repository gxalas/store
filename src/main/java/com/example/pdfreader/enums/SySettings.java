package com.example.pdfreader.enums;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public enum SySettings {
    PATH_TO_FOLDER("appfiles/pdfs");
    public SimpleStringProperty path = new SimpleStringProperty("appfiles/pdfs");

    SySettings(String path){
        this.path.set(path);
    }
    public static SySettings fromName(String name, String attribute) {
        for (SySettings setting : SySettings.values()) {
            System.out.println(setting.name()+" "+setting.getPath());
            if (name.compareTo("PATH_TO_FOLDER")==0) {
                if(Files.exists(Paths.get(attribute))){
                    setting.setPath(attribute);
                } else {
                    System.out.println("the saved in settings path do not exist");
                }
               return setting;
            }
        }
        throw new IllegalArgumentException("No enum constant: " + name);
    }



    public String getPath(){
        return path.get();
    }
    public void setPath(String path){
        this.path.set(path);
    }
}
