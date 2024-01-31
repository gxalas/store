package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.ObservableListDeserializer;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.module.SimpleModule;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.FileReader;
import java.io.IOException;

public class Serialization {






}
