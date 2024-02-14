package com.example.pdfreader.TypesOfDocuments;

import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ABServiceInvoice {
    public static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

    public static boolean isValid(String content){
        String[] lines = content.split("\\r?\\n");
        return lines[1].compareTo("ΠΡΩΤΟΤΥΠΟ") == 0;
        //return lines[2].compareTo("Τιμολόγιο Παροχής Υπηρεσιών") == 0;
    }
    public static void process(Document document, HelloController controller){
        String trimmed = TextExtractions.removeEmptyLines(document.getParts().get(0));
        String[] lines = trimmed.split("\\r?\\n");
        if(lines[2].compareTo("Τιμολόγιο Παροχής Υπηρεσιών") == 0){
            document.setSubType("rent for : "+getStore(lines[17]));
            document.setStore(StoreNames.NONE);
            processRentInvoice(document,lines,controller);
        } else if(lines[2].compareTo("Πιστωτικό Τιμολόγιο")==0){
            document.setSubType("Pistotiko Timologio");
            processPistotiko(document,lines,controller);
        }
    }

    private static void processRentInvoice(Document document,String[] lines, HelloController controller){
        document.setType(ABInvoiceTypes.TIMOLOGIO);
        document.setDocumentId(lines[13]);
        document.setDate(getDate(lines[14]));
        //document.setStore(getStore(lines[17]));
        document.setPromType(PromTypes.AB);
        DocEntry entry = getRentEntry(lines);
        //--8
        entry.setMaster("rent - from service");
        entry.setCode("rent code - from service");
        controller.listManager.docEntriesDescriptions.put(entry.getMaster(),"rent inv wtf");

        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.getProductByMasterCode("009");
        if(product==null){
            product = new Product("009",lines[16],"009");
        }
        entry.setProduct(product);

        retDocumentSums(document,entry);
        document.getEntries().add(entry);

        //controller.listManager.addToImported(document);
        //System.out.println(trimmed);
        removeUnnecessaryData(document);
    }
    private static void processPistotiko(Document document, String[] lines,HelloController controller){
        document.setType(ABInvoiceTypes.PISTOTIKO);
        document.setDocumentId(lines[13]);
        document.setDate(getDate(lines[14]));
        document.setStore(StoreNames.NONE);
        document.setPromType(PromTypes.AB);
        DocEntry entry = getRentEntry(lines);

        //--8
        entry.setMaster("rent - from service");
        entry.setCode("rent code - from service");
        controller.listManager.docEntriesDescriptions.put(entry.getMaster(),"pistotiko wtf");


        ProductDAO productDAO = new ProductDAO();
        Product product = productDAO.getProductByMasterCode("009");
        if(product==null){
            product = new Product("009",lines[16],"009");
        }
        entry.setProduct(product);

        /*
        if(controller.listManager.getProduct("900")==null){
             new Product(controller.listManager,"900",lines[16],"900");
        }
        entry.setProduct(controller.listManager.getProduct("900"));
         */

        retDocumentSums(document,entry);
        document.getEntries().add(entry);
        //controller.listManager.addToImported(document);
        removeUnnecessaryData(document);
    }

    private static StoreNames getStore(String line){
        if (line.startsWith("ΕΝΟΙΚΙΟ ΓΙΑ ΤΟ ΑΚΙΝΗΤΟ 25η ΜΑΡΤΙΟΥ")){
            return StoreNames.DRAPETSONA;
        } else {
            return StoreNames.PERISTERI;
        }
    }
    private static Date getDate(String line){
        try {
            return format.parse(line);
        } catch (ParseException e) {throw new RuntimeException(e);}
    }
    private static DocEntry getRentEntry(String[] lines){
        DocEntry de = new DocEntry();
        de.setVatPercent(new BigDecimal("24.0"));


        for(String line : lines){
            if (line.startsWith("ΚΑΘΑΡΗ ΑΞΙΑ ")){
                String[] words = line.split("\\s+");
                String amount = TextExtractions.convert(words[2]);
                //System.out.println("the amount "+amount);
                de.setNetValue(new BigDecimal(amount));
            }
            if (line.startsWith("ΑΞΙΑ ΦΠΑ")){
                String[] words = line.split("\\s+");
                String amount = TextExtractions.convert(words[2]);
                //System.out.println("the amount "+amount);
                de.setVatValue(new BigDecimal(amount));
            }

            if (line.startsWith("ΠΛΗΡΩΤΕΟ ΣΕ ΕΥΡΩ")){
                line = line.replaceAll("[^\\d,.]", "");
                line = TextExtractions.convert(line);
                //System.out.println("- - - - - - - - - - - - - : "+line);
                de.setTotalPrice( new BigDecimal(line));
                break;
            }
        }

        return de;
    }
    private static void retDocumentSums(Document doc, DocEntry entry){
        doc.setSumEntries(entry.getNetValue().add(entry.getVatValue()));
        doc.setSumRetrieved(entry.getTotalPrice());
    }
    public static void removeUnnecessaryData(Document doc){
        doc.setParts(null);
    }
}
