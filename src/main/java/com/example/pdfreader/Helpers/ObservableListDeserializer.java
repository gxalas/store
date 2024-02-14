package com.example.pdfreader.Helpers;

import com.example.pdfreader.Entities.Main.Document;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;

public class ObservableListDeserializer extends JsonDeserializer<ObservableList<Document>> {

    @Override
    public ObservableList<Document> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<Document> list = p.readValueAs(new TypeReference<List<Document>>() {});
        return FXCollections.observableArrayList(list);
    }
}
