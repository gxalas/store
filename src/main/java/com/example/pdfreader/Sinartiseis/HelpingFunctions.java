package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.HelloController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class HelpingFunctions {
    private static Long start;
    private static Long end;
    public static void printAllDocumentIds(HelloController controller){
        System.out.println(" - - - - PRINTING DOCUMENT IDS - - - -");
        for (Document doc:controller.listManager.getImported()){
            System.out.println(doc.getFilePath()+" has an id "+doc.getDocumentId()+ " is numeric : "+isNumeric(doc.getDocumentId()));
        }
        System.out.println(" - - - - - - - - - - - - - - - - - - -");
    }

    public static void printHeaderOfAFile(String key){
        System.out.println(" - - - - HEADER OF FILE - - - -");
        System.out.println(" - - - - - - - - - - - - - - - - - - -");
    }

    public static boolean isNumeric(String text){
        return text.matches("\\d+");
    }

    public static void setStartTime(){
        start = System.nanoTime();
    }
    public static void setEndAndPrint(String text){
        end = System.nanoTime();
        printTimeDiff(text);
    }
    private static void printTimeDiff(String text){
        Double diff = (end-start)/1_000_000_000.0;
        String d = String.format("%.3f", diff);
        System.out.println(text+": "+d);
    }

    public static void createFileIfNotExists(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path); // This will create the file.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    Date and time methods
     */
    public static Date convertLocalDateToDate(LocalDate localDate) {
        // Assuming you want to start the day at the start of the day in the system's default timezone
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    public static LocalDate convertDateToLocalDate(Date date) {
        // Convert Date to LocalDate at the system's default timezone
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

}
