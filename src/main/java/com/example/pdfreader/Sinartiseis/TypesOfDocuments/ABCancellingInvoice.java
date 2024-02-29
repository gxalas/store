package com.example.pdfreader.Sinartiseis.TypesOfDocuments;

import com.example.pdfreader.Entities.ChildEntities.DocLine;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;

public class ABCancellingInvoice {
    public static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

    public static boolean isValid(String content) {
        return Objects.requireNonNull(ABUsualInvoice.
                        getSecondLine(TextExtractions.removeStartingEmptyLines(content))).
                startsWith("ΑΚΥΡΩΤΙΚΌ ΤΙΜΟΛΟΓΙΟΥ ΠΩΛΗΣΗΣ");
    }

    public static void process(Document doc, HelloController controller) {
        //System.out.println(TextExtractions.removeEmptyLines(doc.parts[doc.parts.length-1]));
        String[] lines = TextExtractions.removeEmptyLines(doc.getParts().get(0)).split("\n");
        String[] lastLines = TextExtractions.removeEmptyLines(doc.getParts().get(doc.getParts().size()-1)).split("\n");
        doc.setSubType("cancelling invoice");
        doc.setDate(getDate(lines));
        doc.setStore(retStore(lines));
        doc.setPromType(PromTypes.AB);
        doc.setType(ABInvoiceTypes.PISTOTIKO);
        doc.setDocumentId(lines[4]);
        doc.setSumRetrieved(new BigDecimal(TextExtractions.convert(lastLines[lastLines.length-3])));
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

    private static StoreNames retStore(String[] lines) {
        String storeCode;
        for (String line : lines) {
            Matcher matcher = DocLine.STORE_CODE_PATTERN.matcher(line);
            if (matcher.matches()) { // Check if current line matches the pattern
                storeCode = matcher.group(2); // Return the second captured group (the alphanumeric part)
                return StoreNames.fromString(storeCode);
                //System.out.println("the store is "+store);
            }
        }
        return StoreNames.NONE;
    }

}
