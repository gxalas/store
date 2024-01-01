package com.example.pdfreader;

import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Sinartiseis.TextExtractions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DocLine {
    public Document document;
    public String productId;
    public String description;
    public List<String> numericValues = new ArrayList<>();

    public Product product;
    private static final Pattern PATTERN_LINE = Pattern.compile(
            "(\\d+)\\s+" +                                                  // Product Code
                    "(.*?)" +                                                      // Product Description (greedy until the next segment)
                    "(?=\\s*\\(\\d+\\)\\s*\\d+|\\s+\\d+\\s+\\d+[,\\.])" +         // Positive lookahead
                    "(?:\\s*\\(([a-zA-Z0-9]*\\s?\\.?[a-zA-Z0-9]*\\s?[a-zA-Z0-9]*)\\))?" + // Optional additional value
                    "(?:\\s*\\((\\d+)\\))?\\s*" +                                  // Optional product additional code (in parentheses)
                    "(\\d+(?:\\.[\\d]{1,3}(?=[\\s,]))?)" +                        // Numeric value 1
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +        // Optional Numeric value 2
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?" +
                    "(?:\\s+([\\d\\.]+,\\d+|[\\d\\.]{1,4}(?=[\\s,])))?"
    );

    private static final Pattern PATTERN_EAN_LINE = Pattern.compile("^\\(\\d+\\)$");
    private static final Pattern SIMPLE_PATTERN_LINE = Pattern.compile("^(?:\\d{11} [Α-ΩΪΫ]+ )?\\d{1,4} \\d [0-9,]+$");// Numeric value 2
    private static final Pattern PATTERN_ORIGIN_LINE = Pattern.compile(
            "([a-zA-Z\\d\\\\.]+)\\s+" +              // Added the period
                    "([Α-ΩΪΫa-zA-ZΩ∆\\w\\s\\.\\/]+)\\s+" +
                    "(\\d+)\\s+" +
                    "([\\d,\\.]+)"
    );

    public static final Pattern DATE_PATTERN = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}"); // DD.MM.YYYY
    public static final Pattern STORE_CODE_PATTERN = Pattern.compile("(\\d+)\\s+([a-zA-Z0-9]+)");

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([\\d,\\.]+)");

    public DocLine(String line, Document doc){
        extractLine(line);
        document = doc;
    }

    public DocLine(String line, Document doc,Boolean metaforiko){
        productId = "0000";
        description = "ΜΕΤΑΦΟΡΙΚΑ";
        List<String> numbers = new ArrayList<>();
        Matcher matcher = NUMERIC_PATTERN.matcher(line);

        while (matcher.find()) {
            numbers.add(matcher.group(1));
        }
        numericValues.add("0000");//
        numericValues.add("0000");//
        numericValues.add("0");//boxes
        numericValues.add("0");//units per box
        numericValues.add("0");//units
        numericValues.add("0");//unit price
        numericValues.add(TextExtractions.convert(numbers.get(0)));//total price
        numericValues.add(TextExtractions.convert(numbers.get(1)));//net
        numericValues.add(TextExtractions.convert(numbers.get(2)));//vat
        numericValues.add(TextExtractions.convert(numbers.get(3)));//percent
        document = doc;
    }
    public void extractLine(String line){
        Matcher matcher = PATTERN_LINE.matcher(line);
        if (matcher.find()) {
            String productCode = matcher.group(1);
            String productDescription = matcher.group(2);
            for (int i = 3; i <= 13; i++) {
                if (matcher.group(i) != null) {
                    String val = matcher.group(i);
                    String converted = val.replace(".", "").replace(',', '.');
                    numericValues.add(converted);
                } else {
                    numericValues.add("0.0");  // Default value if missing
                }
            }
            productId = productCode; //to delete
            description = productDescription; //to delete
        }
    }
    public static boolean matchesEANLine(String line) {
        return PATTERN_EAN_LINE.matcher(line).matches();
    }

    public static boolean matchesORIGINLine(String line){
        return PATTERN_ORIGIN_LINE.matcher(line).matches();
    }

    public static boolean matchesLinePattern(String line){
        return PATTERN_LINE.matcher(line).find();
    }
    public static boolean matchesGreekPattern(String line) {
        return line.split("\\s+").length == 1 && !line.isEmpty();
        //return line.compareTo("ΕΛΛΗΝΙΚΗΣΠΑ")==0;
    }
    public static boolean startWithELLINISPA(String line){
        return line.startsWith("ΕΛΛΗΝΙΚΗΣΠΑ")&&(line.split("\\s").length<=3);
    }

    public void addGreekLineData(String line){
        description +=" & "+line;
    }

    public static boolean matchesSimpleLine(String line){
        return SIMPLE_PATTERN_LINE.matcher(line).matches();
    }

    public void addSimpleLineData(String line){
        Matcher matcher = SIMPLE_PATTERN_LINE.matcher(line);
        if (matcher.find()) {
            String productCode = matcher.group(1);
            String productPlace = matcher.group(2).trim();
            String box = matcher.group(3).trim();
            String units = matcher.group(4).trim();
            description+=" & "+productCode+" & "+productPlace;
        }
    }



    public String getProductId(){
        //if (product!=null){
            //return product.getCode();
        //}
        return productId;
    }
    public String getDescription(){
        if(product!=null){
            return product.getDescription();
        }
        return description;
    }
    public String getRetEan(){
        return numericValues.get(0);
    }
    public String getVal0(){
        return numericValues.get(1);
    }
    public String getVal1(){
        return numericValues.get(2);
    }
    public String getVal2(){
        return numericValues.get(3);
    }
    public String getVal3(){
        return numericValues.get(4);
    }
    public String getVal4(){
        return numericValues.get(5);
    }
    public String getVal5(){
        return numericValues.get(6);
    }
    public String getVal6(){
        return numericValues.get(7);
    }
    public String getVal7(){
        return numericValues.get(8);
    }
    public String getVal8(){
        return numericValues.get(9);
    }
    public String getVal9(){
        return numericValues.get(10);
    }
    public String getDocId(){
        return document.getDocumentId();
    }
    public Document getDocument(){
        return this.document;
    }
    public String getDocPath(){
        return document.getPath();
    }
    public boolean hasProduct(){
        return product != null;
    }
}
