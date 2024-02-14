package com.example.pdfreader.TypesOfDocuments;

import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Sinartiseis.TextExtractions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ABSupportingCancellation {
    public static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

    public static boolean isValid(String content) {
        return Objects.requireNonNull(ABUsualInvoice.
                        getSecondLine(TextExtractions.removeStartingEmptyLines(content))).
                startsWith("ΑΚΥΡΩΤΙΚΟ∆ΙΚΑΙΟΛΟΓΗΤΙΚΟΥ");
    }
    public static void process(Document doc){
        doc.setSubType("cancelling support");
        String[] lines = TextExtractions.removeEmptyLines(doc.getParts().get(0)).split("\n");
        doc.setDate(getDate(lines));
        doc.setDocumentId(lines[4]);
    }

    private static Date getDate(String[] lines) {
        for (String line : lines) {
            try {
                return format.parse(line); // You can return the matched line or the parsed Date object
            } catch (ParseException e) {
                // Ignore parsing exceptions and continue searching
            }
        }
        System.out.println("couln't find a date");
        return null; // Return null if no matching date is found
    }
}
