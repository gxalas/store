package com.example.pdfreader.Helpers;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.PosEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ListManager {
    private final ObservableList<Document> imported = FXCollections.observableArrayList();
    private final ObservableList<Document> failed = FXCollections.observableArrayList();
    private final ObservableQueue<File> filesInFolderQueue = new ObservableQueue<>();
    private final ObservableQueue<Document> toImportQueue = new ObservableQueue<>();
    private final HashMap<UUID, PosEntry> posEntriesMap = new HashMap<>();
    private final HashMap<String,Product> productHashMap = new HashMap<>();
    private List<String> checksums = new ArrayList<>();
    private final ObservableList<MyTask> activeTasksList = FXCollections.observableArrayList();
    private List<String> fileChecksums = new ArrayList<>();

    public ListManager(){

    }
    public ObservableList<Document> getImported() {
        return imported;
    }
    public ObservableList<Document> getFailed() {
        return failed;
    }
    public ObservableQueue<File> getFilesInFolderQueue(){return filesInFolderQueue;}
    public ObservableQueue<Document> getToImportQueue() {return toImportQueue;}
    public void addToImported(Document document) {
        imported.add(document);
    }
    public void addToFailed(Document document) {
        failed.add(document);
    }
    public void addToFilesInFolder(File file){filesInFolderQueue.add(file);}
    public void addToImportQueue(Document doc){toImportQueue.add(doc);}
    public Document getDocument(String code){
        for(Document doc : failed){
            if(doc.getDocumentId().compareTo(code)==0){
                return doc;
            }
        }
        for(Document doc : imported){
            if(doc.getDocumentId().compareTo(code)==0){
                return doc;
            }
        }
        System.out.println("no document found for the code :"+code);
        return null;
    }

    public void loadProductHashMap(){
        productHashMap.clear();
        ProductDAO productDAO = new ProductDAO();
        List<Product> productList = productDAO.getAllProducts();
        for(Product product : productList){
            productHashMap.put(product.getInvmaster(),product);
        }
        System.out.println("- - - - - - - - - -the product map is being initialized - - - - - - - - - - - - "+productHashMap.size());
    }
    public void fetchChecksums(){
        System.out.println("- - - - - - - - -  the checksums are being initialised - - - - - - - - - - ");
        DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
        DocumentDAO ddao = new DocumentDAO(dbErrorDAO);
        checksums = ddao.getAllChecksums();
    }
    public List<String> getChecksums(){
        return checksums;
    }

    public void loadFileChecksums(){
        System.out.println("- - - - - -  loading file checksums - - - - - - ");
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        fileChecksums = entriesFileDAO.getAllChecksums();
    }
    public List<String> getFileChecksums(){
        return this.fileChecksums;
    }

    public void addChecksum(String checksum){
        checksums.add(checksum);
    }
    public HashMap<String,Product> getProductHashMap(){
        return this.productHashMap;
    }
    public void addToProductHashMap(Product product){
        productHashMap.put(product.getInvmaster(),product);
    }

    public ObservableList<MyTask> getActiveTasksList(){
        return activeTasksList;
    }
    public void addTaskToActiveList(String title,String description,MyTask task){
        task.setMyTitle(title);
        task.setMyDescription(description);
        task.setMyState(MyTaskState.PENDING);
        String text = "adding an item "+activeTasksList.size();
        System.out.println(text);

        Platform.runLater(()->{
            if(activeTasksList.size()>10){
                for(int i=activeTasksList.size()-1;i>5;i--){
                    if(activeTasksList.get(i).getMyState().compareTo(MyTaskState.COMPLETED)==0){
                        System.out.println("removing "+i);

                        activeTasksList.remove(i);
                    }
                }
            }
        });


        Platform.runLater(()->{
            System.out.println(text);
            activeTasksList.add(0,task);
        });



    }



}
