package com.example.pdfreader.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum StoreNames {
    PERISTERI("G996","Peristeri", "0290"),
    DRAPETSONA("G846","Drapetsona", "1047"),
    NONE("N000","CBNP","Brand"),
    ALL("S000","All","0101");

    private final String code;
    private final String name;
    private final String description;

    // Constructor
    StoreNames(String code,String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode(){
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    public static StoreNames fromString(String code) {
        for (StoreNames store : StoreNames.values()) {
            if (store.getCode().equalsIgnoreCase(code)) {
                return store;
            }
        }
        return null;
    }
    public static List<String> stringValues(){
        List<String> strVals = new ArrayList<>();
        for(StoreNames val : values()){
            if(val.getName().compareTo(ALL.getName())!=0){
                strVals.add(val.getName());
            }
        }
        return strVals;
    }
    public static StoreNames getStoreByName(String name){
        for(StoreNames store : StoreNames.values()){
            if(store.getName().compareTo(name)==0){
                return store;
            }
        }
        return null;
    }

}
