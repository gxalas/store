package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.Controllers.InvoicesImportView;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.enums.SySettings;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ProcessingTxtFiles {


    /*
    Here we try to load the folder path for
    where the pdfs are stored
    */
    public static void loadSettings(){
        List<SySettings> settings = new ArrayList<>();
        HelpingFunctions.createFileIfNotExists(SySettings.settingsPath.toFile().getPath());
        try (BufferedReader reader = new BufferedReader(new FileReader(SySettings.settingsPath.toFile().getPath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                SySettings setting = SySettings.fromName(parts[0],parts[1]);
                settings.add(setting);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print the read colors
        for (SySettings color : settings) {
            System.out.println(color.name() + ": " + color.getPath());
        }
    }

    /**
     * Finding the invoices that can be loaded
     * @param directory
     * @param hc
     */
    public static void traceFolder(File directory, HelloController hc){
        if(hc.isThereActiveTracing()){
            System.out.println("there is already an active tracing event");
            return;
        }
        TracingFolderEvent tfe = new TracingFolderEvent(hc);
        hc.fireStartTracingFolder(tfe);

        HelpingFunctions.createFileIfNotExists(directory.getPath());
        hc.listManager.getFailed().clear();
        hc.listManager.getChecksums().clear();
        hc.listManager.fetchChecksums();

        filetracing(directory,hc);
        System.out.println("the files in folder "+hc.listManager.getFilesInFolderQueue().size());

        hc.numFilesInFolder.set(hc.listManager.getFilesInFolderQueue().size());

        int i=0;
        while (!hc.listManager.getFilesInFolderQueue().isEmpty()){
            i++;
            File tempFile = hc.listManager.getFilesInFolderQueue().poll();
            TextExtractions.checkFile(tempFile,hc.listManager);
            hc.percentOfTracing.set((double) i / hc.numFilesInFolder.get());
        }
        hc.fireEndTracingFolder(tfe);

    }
    public static void filetracing(File directory, HelloController hc){
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    filetracing(file, hc);
                } else {
                    hc.listManager.addToFilesInFolder(file);
                }
            }
        }
    }
    public static void loadDocuments(HelloController helloController,InvoicesImportView invoicesImportView){
        MyTask loadDocuments = new MyTask(()->{
            processFiles(helloController,invoicesImportView);
            return null;
        });
        loadDocuments.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Throwable exception = workerStateEvent.getSource().getException();
                if (exception != null) {
                    System.err.println("Background task failed:");
                    exception.printStackTrace();
                }
            }
        });
        helloController.listManager.addTaskToActiveList(
                "loading invoices",
                "trying to load the invoices",
                loadDocuments
        );
    }
    public static void processFiles(HelloController parentDelegate, InvoicesImportView invoicesImportView) {
        parentDelegate.listManager.fetchChecksums();


        //parentDelegate.listManager.loadProductHashMap();

        int size = parentDelegate.listManager.getToImportQueue().size();

        SupplierProductRelationDAO relationDAO = new SupplierProductRelationDAO();
        List<SupplierProductRelation> currentRelations = relationDAO.findAll();

        List<SupplierProductRelation> newRelations = new ArrayList<>();

        while (!parentDelegate.listManager.getToImportQueue().isEmpty()){
            invoicesImportView.updateProgressBarFolderLoading(size-parentDelegate.listManager.getToImportQueue().size()+1,size);

            Document newDoc = parentDelegate.listManager.getToImportQueue().poll();

            TextExtractions.process(newDoc,parentDelegate);
            /*
            if(newDoc.getDocumentId().compareTo("9033568261")==0){
                System.out.println("the document in focus is going to be checked for supplier");
                System.out.println("the current relations are : "+currentRelations.size());
            }
            */

            newRelations.addAll(Document.inferSupplier(currentRelations, newDoc));
            currentRelations.addAll(newRelations);
            /*
            if(newDoc.getDocumentId().compareTo("9033568261")==0){
                newDoc.addToErrorList("this is the one with the error");
            }
            */
        }




        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> sbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();
        Map<String,Product> productMap = new HashMap<>();
        sbas.forEach(sba -> {
            if(sba.getFamily().compareTo("930")!=0){
                productMap.put(sba.getMasterCode(),sba.getProduct());
            }
        });

        ProductDAO productDAO = new ProductDAO();
        List<Product> productList = productDAO.getAllProducts();
        productList.forEach(product -> {
            if(product.getInvmaster()!=null){
                productMap.put(product.getInvmaster(),product);
            }
        });
        Set<Product> toSaveProducts = new HashSet<>();


        List<Document> toSaveDocuments = parentDelegate.listManager.getImported();
        toSaveDocuments.forEach(doc->{
            doc.getEntries().forEach(docEntry->{
                if(productMap.get(docEntry.getMaster())!=null){
                    docEntry.setProduct(productMap.get(docEntry.getMaster()));
                    if(productMap.get(docEntry.getMaster()).getInvDescription().isEmpty()){
                        System.err.println("here is the empty product that we are trying to save later "+docEntry.getMaster());
                    }
                } else {
                    Product product = new Product();
                    product.setInvmaster(docEntry.getMaster());
                    String d = "";
                    if(parentDelegate.listManager.docEntriesDescriptions.get(docEntry.getMaster())==null){
                        System.out.println("here we have a null");
                        d = "the null happened";
                        System.out.println("the master"+docEntry.getMaster());

                    } else {

                        d = parentDelegate.listManager.docEntriesDescriptions.get(docEntry.getMaster());
                    }

                    product.setInvDescription(d);
                    product.setCode(docEntry.getCode());
                    docEntry.setProduct(product);
                    toSaveProducts.add(product);
                    productMap.put(product.getInvmaster(),product);

                }
            });
        });
        toSaveProducts.forEach(product -> {
            if(product.getInvDescription().isEmpty()){
                System.err.println("we are trying to save an empty product");
            }
        });

        productDAO = new ProductDAO();
        productDAO.saveProducts(toSaveProducts.stream().toList());


        newRelations.forEach(rel->{
            if(productMap.get(rel.getProduct().getInvmaster())!=null){
                rel.setProduct(productMap.get(rel.getProduct().getInvmaster()));
            }
        });

        DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
        DocumentDAO documentDAO = new DocumentDAO(dbErrorDAO);

        List<DBError> errors = documentDAO.saveDocuments(toSaveDocuments);

        if(!errors.isEmpty()){
            dbErrorDAO.saveDBErrors(errors);
        }
        System.out.println("the total new relations to be saved is "+newRelations.size());
        if(!newRelations.isEmpty()){
            relationDAO.saveAll(newRelations);
        }

        System.out.println("the imported are "+parentDelegate.listManager.getImported().size());
        parentDelegate.listManager.getImported().clear();
        //parentDelegate.listManager.getProductHashMap().clear();
        parentDelegate.listManager.docEntriesDescriptions.clear();
        parentDelegate.listManager.getChecksums().clear();

        parentDelegate.fireDocumentProcessedEvent(new DocumentsImportedEvent(invoicesImportView));
    }

}