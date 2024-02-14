package com.example.pdfreader.Helpers;

import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentDeserializer extends JsonDeserializer<Document> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public Document deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        // Extract values from the JSON node

        String path = node.get("path").asText();
        Document document = new Document(path);

        if(node.has("checksum")){
            document.setChecksum(node.get("checksum").asText());
        } else {System.out.println("no checksum");}

        if(node.has("documentId")){
            document.setDocumentId(node.get("documentId").asText());
        } else {System.out.println("no document Id");}

        List<DocEntry> entries = new ArrayList<>();
        ArrayNode entriesNode = (ArrayNode) node.get("entries");
        ObjectMapper mapper = new ObjectMapper();
        for (JsonNode entryNode : entriesNode) {
            DocEntry entry = mapper.treeToValue(entryNode, DocEntry.class);
            entries.add(entry);
        }


        //boolean duplicate = node.get("duplicate").asBoolean();
        //String checksum = node.get("checksum").asText(); // Deserialize the 'checksum' field
        //String documentId = node.get("documentId").asText();

        // Add more fields extraction and setters as needed
        // Deserialize the 'entries' field as an array of DocEntry


        // Deserialize the 'importDate' field from the formatted date string to Date
        Date importDate;
        try {
            importDate = DATE_FORMAT.parse(node.get("importDate").asText());
        } catch (ParseException e) {
            // Handle the parsing exception
            importDate = new Date(); // Set a default date or handle the error in a suitable way
        }

        Date date;
        try {
            date = DATE_FORMAT.parse(node.get("date").asText());
        } catch (ParseException e) {
            // Handle the parsing exception
            date = new Date(); // Set a default date or handle the error in a suitable way
        }

        if(node.has("unitSum")){
            document.setUnitSum(new BigDecimal(node.get("unitSum").asText()));
        } else {System.out.println("no unit sum");}

        if(node.has("valSum")){
            document.setValSum(new BigDecimal(node.get("valSum").asText()));
        } else {System.out.println("no val sum");}

        if(node.has("netSum")){
            document.setNetSum(new BigDecimal(node.get("netSum").asText()));
        } else {System.out.println("no net sum");}

        if(node.has("vatSum")){
            document.setVatSum(new BigDecimal(node.get("vatSum").asText()));
        } else {System.out.println("no vat sum");}

        if(node.has("sumEntries")){
            document.setSumEntries(new BigDecimal(node.get("sumEntries").asText()));
        } else {System.out.println("no sum entries");}

        if(node.has("sumRetrieved")){
            document.setSumRetrieved(new BigDecimal(node.get("sumRetrieved").asText()));
        } else {System.out.println("no sum retrieved");}


        //BigDecimal unitSum = new BigDecimal(node.get("unitSum").asText());
        //BigDecimal valSum = new BigDecimal(node.get("valSum").asText());
        //BigDecimal netSum = new BigDecimal(node.get("netSum").asText());
        //BigDecimal vatSum = new BigDecimal(node.get("vatSum").asText());

        if(node.has("type")){
            document.setType(ABInvoiceTypes.valueOf(node.get("type").asText()));
        } else {System.out.println("no type");}

        if(node.has("store")){
            document.setStore(StoreNames.valueOf(node.get("store").asText()));
        } else {System.out.println("no store");}

        if(node.has("promType")){
            document.setPromType(PromTypes.valueOf(node.get("promType").asText()));
        } else {System.out.println("no prom");}

        // Deserialize the 'type' field from a string to ABInvoiceTypes enum
        //ABInvoiceTypes type = ABInvoiceTypes.valueOf(node.get("type").asText());
        //StoreNames store = StoreNames.valueOf(node.get("store").asText());
        //PromTypes prom = PromTypes.valueOf(node.get("promType").asText());

        // Create a Document object and set the extracted values

        //document.duplicate=(duplicate);
        //document.setChecksum(checksum);
        //document.setDocumentId(documentId);

        document.setEntries(entries);

        document.setImportDate(importDate);
        document.setDate(date);

        //document.setUnitSum(unitSum);
        //document.setValSum(valSum);
        //document.setNetSum(netSum);
        //document.setVatSum(vatSum);

        //document.setType(type);
        //document.setStore(store);
        //document.setPromType(prom);







        return document;
    }
}

