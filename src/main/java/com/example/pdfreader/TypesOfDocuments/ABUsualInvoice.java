package com.example.pdfreader.TypesOfDocuments;

import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.DocLine;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloApplication;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Sinartiseis.Testers;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ABUsualInvoice {
    public static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    // Define the pattern to match the desired format
    public static Pattern AFM_PATTERN = Pattern.compile("^\\d{1,13} [Α-ΩΪΫ]+ [Α-ΩΪΫ.]+ \\d{1,13}$");
    public static boolean isValidABInvoice(String content){
        return ABInvoiceTypes.isValidDocType(getSecondLine(TextExtractions.removeStartingEmptyLines(content)));
    }
    public static String getSecondLine(String input) {
        int len = input.length();
        int newlinesEncountered = 0;
        int startOfSecondLine = -1;
        int endOfSecondLine = -1;
        for (int i = 0; i < len; i++) {
            if (input.charAt(i) == '\n') {
                newlinesEncountered++;
                if (newlinesEncountered == 1) {
                    startOfSecondLine = i + 1;
                } else {
                    endOfSecondLine = i;
                    break;
                }
            }
        }
        if (newlinesEncountered < 1) {
            return null; // No newlines, so no second line.
        } else if (newlinesEncountered == 1) {
            return input.substring(startOfSecondLine); // Only one newline, so return everything after it.
        } else {
            return input.substring(startOfSecondLine, endOfSecondLine); // Return just the second line.
        }
    }
    public static void process(Document document, HelloController controller){
        retrieveLines(document,controller.listManager);
        //convertLinesToEntries(document,controller.listManager);
        calculateEntriesSum(document);
        //addChecksum(document,controller);
        addToImported(document,controller);
        updateMaxMinDates(document);
        removeUnecessaryData(document);
        if (!checks(document)){
            document.getErrorList().add("not Valid");
        }
    }

    private static void removeUnecessaryData(Document document) {
        document.setParts(null);
    }

    public static void retrieveLines(Document document, ListManager listManager){
        StringBuilder newContent= new StringBuilder();
        for(int i=0;i<document.getParts().size();i++){
            String header = ABUsualInvoice.getHeader(document.getParts().get(i));
            if(header.compareTo("ERROR")==0){
                document.setType(null);
                System.out.println("the file : "+document.getFilePath()+" is not of a valid format");
                return;
            }
            header = TextExtractions.removeStartingEmptyLines(header);
            if(i==0){ // if it is the first page
                retDocumentId(header,document);
                retType(header,document);
                document.setSubType(document.getType().name());
                retDate(header,document);
                retPromType(header,document);
                retStore(header,document);
            }
            String woHeader = document.getParts().get(i).replace(header,"");
            String footer = ABUsualInvoice.getFooter(woHeader);
            String content = woHeader.replace(footer,"");
            content = TextExtractions.removeStartingEmptyLines(content);
            if(i==document.getParts().size()-1){ // if it is the last page
                content = TextExtractions.removeLastLine(content);
                retSums(footer,document);
            }
            newContent.append(content);
        }
        if(document.getDocumentId().compareTo("9033407054")==0){
            //System.out.println(newContent);
        }
        ArrayList<DocLine> docLines = ABUsualInvoice.getDocEntries(newContent.toString(),document);
        convertLinesToEntries(docLines,listManager);

        //document.lines.addAll(docLines);
    }
    public static void convertLinesToEntries(ArrayList<DocLine> docLines, ListManager listManager){
        for(DocLine line: docLines){
            DocEntry entry = new DocEntry(line,listManager); //convert lines to entries
            line.document.getEntries().add(entry);
        }

    }
    public static String getHeader(String content) {
        String header = "";
        String endMarker = "(ΠΑΡΤΙ∆Α) (ΠΡΟΕΛ.-ΠΟΙΚΙΛIA/ΑΛ.ΕΡΓ.-ΚΑΤ) Κ/Β ΠΕΡ.Κ/Β ΜΟΝΑ∆ΟΣ ΑΞΙΑΣ % ΑΞΙΑ ΑΞΙΑ ΠΟΣΟ %";
        int endIndex = content.indexOf(endMarker);
        if (endIndex != -1) {
            header = content.substring(0, endIndex + endMarker.length());
            return header;
        } else {
            System.err.println("Section not found.");
            return "ERROR";
        }
    }
    public static String getFooter(String woHeader) {
        String footer = "";
        String startMarker = "ΓΕΝΙΚΑΣΥΝΟΛΑ:";
        int startIndex = woHeader.indexOf(startMarker);
        if (startIndex != -1) {
            footer = woHeader.substring(startIndex);
            //fullContent = fullContent.replace(endSection,"");
            return footer;
        } else {
            System.err.println("Start marker not found.");
            return "error at footer";
        }
    }
    public static ArrayList<DocLine> getDocEntries(String content, Document doc) {
        String[] lines = content.split("\n");
        ArrayList<DocLine> entries = new ArrayList<>();
        boolean test = false;
        for (int i = 0; i < lines.length; i++) {

            try {
                if (DocLine.matchesEANLine(lines[i].trim())) {
                    String modifiedString = lines[i].replaceAll("[^0-9]", ""); // keep all numeric characters
                    entries.get(entries.size() - 1).numericValues.set(0, modifiedString);
                    continue;
                }
                if (DocLine.matchesGreekPattern(lines[i].trim())) {
                    entries.get(entries.size() - 1).addGreekLineData(lines[i]);
                    continue;
                }
                if(DocLine.startWithELLINISPA(lines[i].trim())){
                    entries.get(entries.size()-1).addGreekLineData(lines[i]);
                    continue;
                }
                if (DocLine.matchesSimpleLine(lines[i].trim())) {
                    entries.get(entries.size() - 1).description += " & " + lines[i];
                    continue;
                }
                if (DocLine.matchesORIGINLine(lines[i].trim())) {
                    continue;
                }
                if(lines[i].trim().startsWith("ΜΕΤΑΦΟΡΙΚΌΚΌΣΤΟΣ")){
                    DocLine docLine = new DocLine(lines[i],doc, true);
                    entries.add(docLine);
                    continue;
                }
                if (DocLine.matchesLinePattern(lines[i].trim())) {
                    DocLine docLine = new DocLine(lines[i], doc);
                    entries.add(docLine);
                    continue;
                }

                if ((lines[i].trim()).isEmpty()) {
                    //System.out.println("error at parsing a line : " + lines[i] + " at " + doc.path.getName()+" seems to be empty");
                    continue;
                }
                doc.getErrorList().add("problem with a line");
                System.out.println("couldn't parse a line : "+lines[i]+" at doc "+doc.getFilePath());


            } catch (Exception e) {
                System.out.println("the line : " + lines[i]);
                System.out.println("from : " + content);
                System.out.println("threw :" + e);
            }
        }
        return entries;
    }
    public static void retDocumentId(String header, Document document){
        String[] lines = header.split("\n");
        int i=0;
        for(String line:lines){
            if (line.compareTo("ΑΡΙΘΜΟΣΠΑΡ/ΚΟΥ ΑΥΤΟΚΙΝΗΤΟ")==0){
                document.setDocumentId(lines[i+1]);
                break;
            }
            i++;
        }
        if(lines.length==i){
            System.err.println("couldn't get a document id");
        }
    }
    public static void retStore(String header,Document document){
        String storeCode;
        String[] lines = header.split("\n");
        for (String line : lines) {
            Matcher matcher = DocLine.STORE_CODE_PATTERN.matcher(line);
            if (matcher.matches()) { // Check if current line matches the pattern
                storeCode =  matcher.group(2); // Return the second captured group (the alphanumeric part)
                document.setStore(StoreNames.fromString(storeCode));
                //System.out.println("the store is "+store);
            }
        }
    }
    public static void retType(String header,Document document){
        String[] lines = header.split("\n");
        document.setType(ABInvoiceTypes.fromString(lines[1]));
    }
    public static void retPromType(String header,Document document){
        String[] lines = header.split("\n");
        int i=0;
        for(String line:lines){
            if (line.compareTo("ΠΕΛΑΤΗΣ: ΠΑΡΑΛΗΠΤΗΣ:")==0){
                if(lines[i+1].startsWith("ΤΡΙ")){
                    document.setPromType(PromTypes.TRIGONIKOI);
                } else {
                    document.setPromType(PromTypes.AB);
                }
                break;
            }
            i++;
        }
        if(lines.length==i){
            System.err.println("couldn't get a document id");
        }
    }
    public static void retDate(String header,Document document){
        String[] lines = header.split("\n");
        for (String line : lines) {
            Matcher matcher = DocLine.DATE_PATTERN.matcher(line);
            if (matcher.find()) {
                String dateString = matcher.group();
                try {
                    document.setDate(format.parse(dateString));
                } catch (ParseException e) {
                    e.printStackTrace();
                    document.setDate(null);
                }
            }
        }
    }
    public static void retSums(String footer, Document document){
        String[] lines = footer.split("\n");
        String fLine = TextExtractions.findLineStartingWith(lines, "ΓΕΝΙΚΑΣΥΝΟΛΑ:");
        int sumLine = findSumLine(lines);
        String sLine = TextExtractions.findLineStartingWith(lines, "ΑΞΙΑ");
        if (fLine!=null){
            String[] firstLine = fLine.split("\\s+");
            document.setUnitSum(new BigDecimal(TextExtractions.convert(firstLine[1])));
            document.setValSum(new BigDecimal(TextExtractions.convert(firstLine[2])));
            document.setNetSum(new BigDecimal(TextExtractions.convert(firstLine[3])));
            document.setVatSum(new BigDecimal(TextExtractions.convert(firstLine[4])));
            if (sumLine!=-1){
                //System.out.println("- - - - - - - ->"+TextExtractions.convert(lines[sumLine]));
                document.setSumRetrieved(new BigDecimal(TextExtractions.convert(lines[sumLine]).trim()));
                //System.out.println("the retrieved value is "+document.sumRetrieved);
                if(document.getNetSum().add(document.getVatSum()).compareTo(document.getSumRetrieved()) != 0) {
                    System.err.println("something is wrong in calculating the sum");
                }
            } else {
                System.out.println("not found is "+document.getDocumentId());
                document.setSumRetrieved(BigDecimal.ZERO);
                System.out.println(footer);
            }
        }
        if (document.getSumRetrieved() ==null){
            System.out.println("---------------------------> sum is null "+document.getDocumentId());
        }

    }
    public static boolean checks(Document doc){
        if (!checkDocumentId(doc)){
            return false;
        }
        return compareSums(doc);
    }
    public static boolean checkDocumentId(Document doc){
        if(doc.getDocumentId()==null){
            System.err.println("the document id of the file "+doc.getPath()+"is null");
            return false;
        } else if (!Testers.isNumeric(doc.getDocumentId())){
            System.err.println("the document id of the file "+doc.getPath()+"is null");
            return false;
        } else {
            return true;
        }
    }
    public static boolean compareSums(Document document){
        if (document.getSumEntries().compareTo(document.getSumRetrieved())!=0){
            System.err.println("the sum retrieved and calculated aren't equal for :"+document.getDocumentId()+" "+ document.getSumEntries() +" / "+ document.getSumRetrieved());
            return false;
        }
        return true;
    }
    public static void updateMaxMinDates(Document document){
        if(document.getErrorList().isEmpty() && (document.getType()== ABInvoiceTypes.TIMOLOGIO || document.getType()== ABInvoiceTypes.PISTOTIKO)){
            calculateMaxMinDate(document.getDate());
        }
    }
    public static void calculateEntriesSum(Document document){
        int i =0;
        BigDecimal temp = BigDecimal.ZERO;
        for(DocEntry doc:document.getEntries()){
            if (doc.getNetValue()!=null && doc.getVatValue()!=null) {
                temp = temp.add(doc.getNetValue().add(doc.getVatValue()));
            } else {
                i++;
                System.out.println(i+" found null in the document with id "+document.getDocumentId());
            }
        }
        //return temp.compareTo(sum)==0;
        document.setSumEntries(temp);
    }

    public static void addToImported(Document doc, HelloController controller){
        //System.out.println("----------------------------------------------------------------> add to imported");
        if (controller.listManager.getImported().contains(doc)){
            System.out.println("Trying to import already imported document - serious logical error");
        } else {
            controller.listManager.addToImported(doc);
        }
    }
    public static void calculateMaxMinDate(Date date){
        if(date==null){
            System.out.println("incoming date is null");
            return;
        }
        if(HelloApplication.minDate==null){
            HelloApplication.minDate=date;
        }
        if(HelloApplication.maxDate==null){
            HelloApplication.maxDate=date;
            return;
        }
        if (date.before(HelloApplication.minDate)){
            HelloApplication.minDate = date;
            //System.out.println(minDate);
            return;
        }
        if (date.after(HelloApplication.maxDate)){
            HelloApplication.maxDate = date;
        }
    }
    public static int findSumLine(String[] lines) {
        // Split the input string into lines
        // Iterate through each line and check for a match
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = AFM_PATTERN.matcher(lines[i]);
            if (matcher.matches()) {
                // Return the line number (1-based index)
                return i+1;
            }
        }

        // Return -1 if no matching line is found
        return -1;
    }
}



