package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloController;

public class Testers {
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

}
