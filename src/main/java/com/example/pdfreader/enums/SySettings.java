package com.example.pdfreader.enums;

import com.example.pdfreader.Sinartiseis.ProcessingTxtFiles;
import javafx.beans.property.SimpleStringProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public enum SySettings {
    PATH_TO_FOLDER("appfiles/pdfs");
    public static final Path txtFolderPath = Paths.get("appFiles/txts");
    public static final Path settingsPath = Paths.get("appFiles/saved/settings.txt");
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

    public static void saveSySettings(){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingsPath.toFile().getPath()))) {
            for (SySettings setting : SySettings.values()) {
                writer.write(setting.name()+","+setting.getPath());
                writer.newLine();
            }
            System.out.println("the settings.txt file has been written");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
