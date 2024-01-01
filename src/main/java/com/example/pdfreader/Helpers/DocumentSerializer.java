package com.example.pdfreader.Helpers;

import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Document;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class DocumentSerializer extends JsonSerializer<Document> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public void serialize(Document document, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("path", document.getPath());
        //jsonGenerator.writeObjectField("importDate", document.getImportDate());
        //jsonGenerator.writeObjectField("date", document.getDate());
        //jsonGenerator.writeBooleanField("duplicate", document.is());
        jsonGenerator.writeStringField("checksum", document.getChecksum());
        jsonGenerator.writeStringField("documentId", document.getDocumentId());

        // Serialize the 'entries' field as an array
        jsonGenerator.writeArrayFieldStart("entries");
        for (DocEntry entry : document.getEntries()) {
            jsonGenerator.writeObject(entry);
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeStringField("importDate", DATE_FORMAT.format(document.getImportDate()));
        jsonGenerator.writeStringField("date", DATE_FORMAT.format(document.getDate()));

        jsonGenerator.writeStringField("unitSum", document.getUnitSum().toString());
        jsonGenerator.writeStringField("valSum", document.getValSum().toString());
        jsonGenerator.writeStringField("netSum", document.getNetSum().toString());
        jsonGenerator.writeStringField("vatSum", document.getVatSum().toString());
        jsonGenerator.writeStringField("sumEntries", document.getSumEntries().toString());
        jsonGenerator.writeStringField("sumRetrieved", document.getSumRetrieved().toString());


        jsonGenerator.writeStringField("type", document.getType().name());
        jsonGenerator.writeStringField("store", document.getStore().name());
        jsonGenerator.writeStringField("promType", document.getPromType().name());



        // Add more fields that you want to serialize
        jsonGenerator.writeEndObject();
    }
}
