package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloController;

public class HelpingFunctions {
    public static Long start;
    public static Long end;
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
    private static void printTimeDiff(Long start,Long end,String text){
        Double diff = (end-start)/1_000_000_000.0;
        String d = String.format("%.3f", diff);
        System.out.println(text+": "+d);
    }

}
