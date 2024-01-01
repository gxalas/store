package com.example.pdfreader.enums;

public enum ABInvoiceTypes {
    TIMOLOGIO("ΤΙΜΟΛΟΓΙΟΠΩΛΗΣΗΣ"),
    PISTOTIKO("ΠΙΣΤΩΤΙΚΟΤΙΜΟΛΟΓΙΟΠΩΛΗΣΕΩΝ"),
    INVALID("");
    private final String line;
    ABInvoiceTypes(String line){
        this.line = line;
    }
    public String getLine(){
        return line;
    }
    public static ABInvoiceTypes fromString(String line) {
        for (ABInvoiceTypes type : ABInvoiceTypes.values()) {
            if (type.getLine().equalsIgnoreCase(line)) {
                return type;
            }
        }
        return null;
    }
    public static boolean isValidDocType(String line){
        for (ABInvoiceTypes type : ABInvoiceTypes.values()) {
            if (type.getLine().equalsIgnoreCase(line)) {
                return true;
            }
        }
        return false;
    }
}
