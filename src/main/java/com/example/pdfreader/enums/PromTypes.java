package com.example.pdfreader.enums;

import java.util.Optional;

public enum PromTypes {
    AB,
    TRIGONIKOI;
    PromTypes(){
    }
    public static Optional<PromTypes> fromString(String name) {
        for (PromTypes type : PromTypes.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
