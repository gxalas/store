package com.example.pdfreader.Sinartiseis.TypesOfDocuments;

import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ABNonPriced {
    public static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    public static boolean isValid(String content){
        return Objects.requireNonNull(ABUsualInvoice.getSecondLine(TextExtractions.
                        removeStartingEmptyLines(content))).
                compareTo("∆ΙΚΑΙΟΛΟΓΗΤΙΚΟΕΓΓΡΑΦΟ∆ΙΑΚΙΝΗΣΗΣΜηΤΙΜΟΛΟΓΗΘΕΝΤΩΝΑΠΟΘΕΜΑΤΩΝ") == 0;
        //return lines[2].compareTo("Τιμολόγιο Παροχής Υπηρεσιών") == 0;
    }
    public static void process(Document doc){
        String trimmed = TextExtractions.removeEmptyLines(doc.getParts().get(0));
        String[] lines = trimmed.split("\\r?\\n");

        //System.out.println(String.join("\n",lines));
        doc.setSubType("non - priced");
        doc.setNetSum(BigDecimal.ZERO);
        doc.setValSum(BigDecimal.ZERO);
        doc.setSumRetrieved(BigDecimal.ZERO);
        doc.setSumEntries(BigDecimal.ZERO);
        doc.setStore(StoreNames.NONE);
        doc.setDate(getDate(lines));
        doc.setType(ABInvoiceTypes.TIMOLOGIO);
        doc.setPromType(PromTypes.AB);
        doc.setDocumentId(lines[4]);
        //System.out.println(doc.parts[0]);
    }
    private static Date getDate(String line){
        try {
            return format.parse(line);
        } catch (ParseException e) {throw new RuntimeException(e);}
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
