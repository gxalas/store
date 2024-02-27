package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Controllers.ByMenu.Invoices.InvoicesImportView;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportPdfFiles {



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

            newRelations.addAll(Document.inferSupplier(currentRelations, newDoc));
            currentRelations.addAll(newRelations);
        }


        List<Document> toSaveDocuments = parentDelegate.listManager.getImported();


        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> sbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();

        Map<String,StoreBasedAttributes> globalMasterToSba = new HashMap<>();
        Map<StoreNames,Map<String,StoreBasedAttributes>> storeMasterToSbaMap = new HashMap<>();

        sbas.forEach(sba->{

            if(sba.getProduct()!=null){

                storeMasterToSbaMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
                storeMasterToSbaMap.get(sba.getStore()).putIfAbsent(sba.getMasterCode(), sba);


                if(globalMasterToSba.get(sba.getMasterCode())!=null){
                    if(!globalMasterToSba.get(sba.getMasterCode()).getDepartment().trim().isEmpty() ||
                    globalMasterToSba.get(sba.getMasterCode()).getDepartment().startsWith("-")){
                        globalMasterToSba.put(sba.getMasterCode(),sba);
                    }
                } else {
                    globalMasterToSba.put(sba.getMasterCode(),sba);
                }

                //productMap.computeIfAbsent(sba.getMasterCode(), k -> sba.getProduct());
            }
        });
        storeMasterToSbaMap.computeIfAbsent(StoreNames.DRAPETSONA, k -> new HashMap<>());
        System.out.println("store master to sba size of drapetsona : "+storeMasterToSbaMap.get(StoreNames.DRAPETSONA).size());
        System.out.println("the global master to sba size = "+globalMasterToSba.size());

        Set<Product> toSaveProducts = new HashSet<>();
        Set<StoreBasedAttributes> toSaveSbas = new HashSet<>();


        //Set<Product> toUpdateProducts = new HashSet<>();

        toSaveDocuments.forEach(document -> {
            for (DocEntry docEntry : document.getEntries()) {

                StoreBasedAttributes currentSba = docEntry.getSba();

                if (storeMasterToSbaMap.get(document.getStore()) != null) {
                    if (storeMasterToSbaMap.get(document.getStore()).get(docEntry.getMaster()) != null) {

                        docEntry.setSba(storeMasterToSbaMap.get(document.getStore()).get(docEntry.getMaster()));
                        //update sba
                        continue;
                    }
                }

                if(globalMasterToSba.get(currentSba.getMasterCode())!=null){
                    StoreBasedAttributes dbSba = globalMasterToSba.get(currentSba.getMasterCode());
                    StoreBasedAttributes newSba = new StoreBasedAttributes();

                    newSba.setStore(currentSba.getStore());
                    newSba.setDescription(dbSba.getDescription());
                    newSba.setMasterCode(currentSba.getMasterCode());
                    newSba.setHope(dbSba.getHope());
                    newSba.setDepartment(dbSba.getDepartment());
                    newSba.setProduct(dbSba.getProduct());
                    newSba.getBarcodes().addAll(currentSba.getBarcodes());
                    newSba.getHopeBarcodes().addAll(currentSba.getHopeBarcodes());
                    newSba.getConflictingBarcodes().addAll(currentSba.getConflictingBarcodes());

                    docEntry.setSba(newSba);
                    toSaveSbas.add(newSba);

                    storeMasterToSbaMap.computeIfAbsent(currentSba.getStore(), k -> new HashMap<>());
                    storeMasterToSbaMap.get(currentSba.getStore()).put(currentSba.getMasterCode(),newSba);

                    continue;
                }
                if(currentSba.getDepartment().isEmpty()){
                    currentSba.setDepartment("-3");
                }


                Product product = new Product();
                product.setInvDescription(currentSba.getDescription());
                product.setInvmaster(currentSba.getMasterCode());
                product.setCode(docEntry.getCode());
                product.setLog("from invoices - docEntry");
                currentSba.setProduct(product);

                //docEntry.getSba().setProduct(product);
                storeMasterToSbaMap.computeIfAbsent(document.getStore(), k -> new HashMap<>());
                storeMasterToSbaMap.get(document.getStore()).computeIfAbsent(docEntry.getSba().getMasterCode(), k -> {
                    return currentSba;
                });
                globalMasterToSba.put(currentSba.getMasterCode(),currentSba);
                toSaveProducts.add(product);
                toSaveSbas.add(docEntry.getSba());
            }
        });

        AtomicInteger i = new AtomicInteger();

        toSaveSbas.stream().toList().forEach(sba->{
            if(sba.getMasterCode().compareTo("40747559900")==0){
                i.getAndIncrement();
                //System.out.println("The product we are looking for is found");
            }
        });
        System.out.println("the sbas with the mastercode are "+i.get());


        toSaveProducts.forEach(product -> {
            if(product.getInvDescription().isEmpty()){
                System.err.println("we are trying to save an empty product");
            }
        });

        ProductDAO productDAO = new ProductDAO();
        productDAO.saveProducts(toSaveProducts.stream().toList());


        storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.saveSBAs(toSaveSbas.stream().toList());


        newRelations.forEach(relation->{
            if(globalMasterToSba.get(relation.getProduct().getInvmaster())!=null){
                relation.setProduct(globalMasterToSba.get(relation.getProduct().getInvmaster()).getProduct());
            }
        });

        DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
        DocumentDAO documentDAO = new DocumentDAO(dbErrorDAO);

        HelpingFunctions.setStartTime();
        List<DBError> errors = documentDAO.saveDocuments(toSaveDocuments);
        HelpingFunctions.setEndAndPrint("saving documents");

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
