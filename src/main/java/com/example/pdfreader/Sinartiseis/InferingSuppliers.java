package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.DBErrorDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.StoreBasedAttributesDAO;
import com.example.pdfreader.DAOs.SupplierProductRelationDAO;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.Supplier;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.enums.ABInvoiceTypes;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InferingSuppliers {


    public static void forAllDocuments(){
        DocumentDAO documentDAO = new DocumentDAO(new DBErrorDAO(new ErrorEventManager()));
        List<Document> allDocuments = documentDAO.getAllDocuments();
        infer(allDocuments);
    }

    public static void infer(List<Document> allDocuments){
        // Load all relations and documents once at the beginning
        SupplierProductRelationDAO relationDAO = new SupplierProductRelationDAO();
        List<SupplierProductRelation> allRelations = relationDAO.findAll();


        System.out.println("there were "+allDocuments.size());
        allDocuments = allDocuments.stream().filter(doc->doc.getType().compareTo(ABInvoiceTypes.TIMOLOGIO)==0).collect(Collectors.toList());
        System.out.println("there are "+allDocuments.size());
        System.out.println("the relations are "+allRelations.size());

        List<Document> toSaveDocuments = new ArrayList<>();
        List<SupplierProductRelation> toSaveSPRs = new ArrayList<>();

        // Convert relations to a Map for easy access
        Map<Product, Set<Supplier>> productSupplierMap = allRelations.stream()
                .collect(Collectors.groupingBy(
                        SupplierProductRelation::getProduct,
                        Collectors.mapping(SupplierProductRelation::getSupplier, Collectors.toSet())
                ));
        //System.out.println("the size of product supplier map : "+allRelations.get(0).getProduct().getDescription()+"and the sups are"+productSupplierMap.get(allRelations.get(0).getProduct()).stream().toList().get(0).getName());

        int iterations = 0;
        int newRelationsCreated;
        do {
            newRelationsCreated = 0;
            for (Document document : allDocuments) {
                Map<Supplier, Integer> supplierFrequency = new HashMap<>();
                for (Product product : document.getProducts()) {
                    Set<Supplier> suppliers = productSupplierMap.getOrDefault(product, new HashSet<>());
                    for (Supplier supplier : suppliers) {
                        supplierFrequency.put(supplier, supplierFrequency.getOrDefault(supplier, 0) + 1);
                    }
                }
                // Find the most common supplier
                Optional<Supplier> mostCommonSupplier = supplierFrequency.entrySet().stream()
                        .filter(entry -> entry.getValue() >= 1)
                        .max(Comparator.comparingInt(Map.Entry::getValue))
                        .map(Map.Entry::getKey);

                if (mostCommonSupplier.isPresent()) {
                    Supplier supplier = mostCommonSupplier.get();
                    document.setSupplier(supplier);
                    toSaveDocuments.add(document);

                    // Check and create new relations
                    for (Product product : document.getProducts()) {
                        if (!productSupplierMap.containsKey(product) || !productSupplierMap.get(product).contains(supplier)) {
                            SupplierProductRelation newRelation = new SupplierProductRelation(product, supplier);
                            //allRelations.add(newRelation);
                            toSaveSPRs.add(newRelation);
                            newRelationsCreated++;
                            productSupplierMap.computeIfAbsent(product, k -> new HashSet<>()).add(supplier);
                        }
                    }
                }
            }
            System.out.println("Iteration " + (iterations + 1) + ": " + newRelationsCreated + " new relations created");
            iterations++;
        } while (newRelationsCreated > 0 && iterations < 20);
        // Perform database updates

        DocumentDAO documentDAO = new DocumentDAO(new DBErrorDAO(new ErrorEventManager()));
        documentDAO.updateDocuments(toSaveDocuments);
        // documentDAO.saveAll(allDocuments);
        relationDAO.saveAll(toSaveSPRs);
    }

    public static void printProductSupplierRelations(HelloController parentDelegate){
        MyTask myTask = new MyTask(()-> null);
        myTask.setTaskLogic(()->{
            Map<String,Product> barcodeToProduct = new HashMap<>();
            Map<Product,List<Supplier>> productToSupplier = new HashMap<>();

            CompletableFuture<Void> loadSBAs = CompletableFuture.runAsync(()->{
                StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                List<StoreBasedAttributes> allSbas = new ArrayList<>(storeBasedAttributesDAO.getAllStoreBasedAttributes());

                allSbas.forEach(sba->{
                    if(sba.getProduct()!=null){
                        sba.getBarcodes().forEach(barcode->{
                            if(barcodeToProduct.get(barcode)!=null) {
                                if(!sba.getProduct().equals(barcodeToProduct.get(barcode))){
                                    System.err.println(" we probably run into a conflict " +
                                            sba.getDescription()+" # "+sba.getFamily()+ " : "+
                                            sba.getProduct().getInvDescription()+" @ "+ sba.getStore()+" <-> "+
                                            barcodeToProduct.get(barcode).getInvDescription());
                                }
                            } else {
                                barcodeToProduct.put(barcode,sba.getProduct());
                            }
                        });
                    }
                });
            });

            CompletableFuture<Void> loadSPRs = CompletableFuture.runAsync(()->{
                SupplierProductRelationDAO supplierProductRelationDAO = new SupplierProductRelationDAO();
                List<SupplierProductRelation> allSPRs = supplierProductRelationDAO.findAll();
                allSPRs.forEach(spr->{
                    productToSupplier.computeIfAbsent(spr.getProduct(), k -> new ArrayList<Supplier>());
                    productToSupplier.get(spr.getProduct()).add(spr.getSupplier());
                });
            });
            CompletableFuture<Void> onCompleteLoading = CompletableFuture.allOf(loadSPRs,loadSBAs);
            onCompleteLoading.thenRun(()->{
                barcodeToProduct.keySet().forEach(barcode->{
                    if(productToSupplier.get(barcodeToProduct.get(barcode))!=null){
                        productToSupplier.get(barcodeToProduct.get(barcode)).forEach(supplier->{
                            System.out.println("the supplier "+supplier.getName()+" supplies barcode "+barcode);
                        });
                    }
                });
                System.err.println("\n\n\n barcode to product size : "+barcodeToProduct.size()+"\n\n\n");
                System.out.println(" - - -  new map ended - - - ");
            });
            return null;
        });
        parentDelegate.listManager.addTaskToActiveList(
                "testing new map",
                "testing the new map",
                myTask
        );
    }
}
