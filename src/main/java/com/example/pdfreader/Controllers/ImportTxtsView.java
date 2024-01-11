package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.EntriesFileDAO;
import com.example.pdfreader.DAOs.HibernateUtil;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.Sinartiseis.Serialization;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.StoreNames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportTxtsView extends ChildController{
    List<Product> toCreate = new ArrayList<>();
    List<Product> toUpdate = new ArrayList<>();
    List<PosEntry> posEntries = new ArrayList<>();
    Map<File, StoreNames> itemFiles = new HashMap<>();
    Map<File, StoreNames> posSaleFiles = new HashMap<>();

    @Override
    public void initialize(HelloController hc) {
        System.out.println("importing text is here");


        readingTxtFilesLogic();

    }


    @Override
    public void addMyListeners() {

    }

    @Override
    public void removeListeners(HelloController hc) {

    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return null;
    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }
    public void readingTxtFilesLogic(){


        /*
        load a map from the database
        barcode->product

         */

        //parentDelegate.listManager.loadFileChecksums();


        getFilesFromFolder();

        AtomicInteger fileNum = new AtomicInteger();
        Arrays.stream(StoreNames.values()).toList().forEach(
                store->{
                    List<File> itemTxts = new ArrayList<>();
                    List<File> posTxts = new ArrayList<>();
                    itemFiles.forEach((f,s)->{
                        if(s.compareTo(store)!=0){
                            return;
                        }
                        itemTxts.add(f);
                    });
                    posSaleFiles.forEach((fi,st)->{
                        if(st.compareTo(store)!=0){
                            return;
                        }
                        posTxts.add(fi);
                    });
                    if(!itemTxts.isEmpty()&&!posTxts.isEmpty()){
                        itemTxts.forEach(txt->{
                            System.out.println("processing "+txt.getName()+" for store "+store);
                        });
                        posTxts.forEach(txt->{
                            System.out.println("processing "+txt.getName()+" for store "+store);
                        });
                    } else {
                        System.out.println("- - - - not all type of files found for store "+store);
                    }
                });

        ProductDAO productDAO = new ProductDAO();
        Product product = new Product();
        product.setDescription("test description");
        product.setMaster("masterTempo");

        StoreBasedAttributes sba = new StoreBasedAttributes();
        sba.setStore(StoreNames.PERISTERI);
        sba.setDescription("a new test sba description");
        sba.setHope("1203434");
        sba.setDepartment("12");
        sba.setMasterCode("0101010101");

        product.getAttributes().put(sba.getStore(),sba);

        System.out.println("the store based attributes are "+product.getAttributes().size());

        //
        productDAO.saveProduct(product);

        //product.set
        //the other thing that we would like to add is the possale check that i think we already
        //have implemented, cause in the item txt file we do not need to implement this, we want to
        //update







    }
    public void getFilesFromFolder(){
        File parentFolder = Serialization.txtFolderPath.toFile();
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] filesAndFolders = parentFolder.listFiles();
            if (filesAndFolders != null) {
                for (File file : filesAndFolders) {
                    if (file.isDirectory()) {
                        for (StoreNames storeName : StoreNames.values()) {
                            if (file.getName().compareTo(storeName.getDescription()) == 0) {
                                List<File> txtFiles = EntriesFile.getTxtFilesInFolder(file.getPath());
                                for (File f : txtFiles) {
                                    if (f.getName().toLowerCase().contains("items")) {
                                        itemFiles.put(f, storeName);
                                    } else if (f.getName().toLowerCase().contains("possales")) {
                                        posSaleFiles.put(f, storeName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void processTxtFiles(){
        for (Map.Entry<File, StoreNames> entry : posSaleFiles.entrySet()) {
            processPosSaleFile(entry.getKey(), entry.getValue());
        }

        saveProducts(toCreate, toUpdate);

        for (Map.Entry<File, StoreNames> entry : itemFiles.entrySet()) {
            processItemFile(entry.getKey(), entry.getValue());
        }

        //the other thing we would like to implement is
        //

        savePosEntries(posEntries);

    }

    private void processPosSaleFile(File posSaleFile, StoreNames storeName) {
        String fChecksum = TextExtractions.calculateChecksum(posSaleFile);
        if (!parentDelegate.listManager.getFileChecksums().contains(fChecksum)) {
            System.out.println("Extracting POS sales file for " + storeName.getDescription());

            posEntries.addAll(extractPosEntries(storeName, posSaleFile, parentDelegate.listManager.getProductHashMap(), toCreate, toUpdate));

            EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
            entriesFileDAO.saveEntriesFile(new EntriesFile(posSaleFile.getPath(), fChecksum));
        } else {
            System.out.println("The file: " + posSaleFile.getName() + " already exists "+fChecksum);
        }
    }
    public  List<PosEntry> extractPosEntries(StoreNames storeName,File file,HashMap<String,Product> products,List<Product> toCreate,List<Product> toUpdate){
        System.out.println("the products are "+products.size());
        List<PosEntry> posEntries = new ArrayList<>();
        Date indexDate = null;
        int index =0;
        try{
            Path path = file.toPath();
            List<String> possales = Files.readAllLines(path, Charset.forName("ISO-8859-7"));
            try {
                for (String possale : possales) {
                    if (possale.trim().compareTo("") != 0) {
                        PosEntry temp = new PosEntry(possale, storeName);
                        if(indexDate==null){
                            indexDate=temp.date;
                        }
                        if(indexDate.compareTo(temp.date)!=0){
                            index =0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;
                        if (products.get(temp.getMaster())!=null){
                            temp.setProduct(products.get(temp.getMaster()));
                            if (!products.get(temp.getMaster()).getDescriptions().contains(temp.description)){
                                if(temp.getDescription().contains("PAL MAL ΚΟΚΚΙΝΟ")){
                                    System.out.println("this is at Serialization "+storeName.getName()+" "+file.getPath());
                                }
                                products.get(temp.getMaster()).setDescription(temp.getDescription());
                                toUpdate.add(products.get(temp.getMaster()));
                            }
                        } else {
                            Product product = new Product(temp.getDescription(), temp.getMaster());
                            products.put(product.getMaster(),product);
                            toCreate.add(product);
                            temp.setProduct(product);
                        }
                        posEntries.add(temp);
                    }
                }
            } catch (Exception e){
                System.out.println("\n\n\n\n\n");
                System.out.println("an error happened while extraction possales");
                System.out.println("\n\n\n\n\n");
                e.printStackTrace();
                System.out.println("\n\n\n\n\n");
                System.err.println(e.getMessage());
            }
            System.out.println("read complete - - - - - ");
        } catch (IOException e){
            System.err.println("Error reading possales \n"+e.getMessage());
            e.printStackTrace();
        }
        System.out.println("the products list is _+_+_+__++++ "+products.size());
        System.out.println("Map hash inside method: " + System.identityHashCode(products));
        return posEntries;
    }
    private void saveProducts(List<Product> toCreate, List<Product> toUpdate) {
        ProductDAO productDAO = new ProductDAO();
        System.out.println("Saving new products: " + toCreate.size());
        productDAO.addNewProducts(toCreate);

        System.out.println("Updating products "+ toUpdate.size());
        productDAO.updateProducts(toUpdate);
        System.out.println("Completed saving products and POS entries");
    }
    private void savePosEntries(List<PosEntry> posEntries){
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        System.out.println("Saving POS entries "+posEntries.size());
        posEntryDAO.savePosEntries(posEntries);
    }
    private void processItemFile(File itemFile, StoreNames storeName) {
        String fChecksum = TextExtractions.calculateChecksum(itemFile);
        System.out.println("extracting items file : "+itemFile.getName());

        extractItemsLines(itemFile,storeName);

        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(itemFile.getPath(), fChecksum));
    }
    public void extractItemsLines(File file,StoreNames store){

        //Map<String,Product> productMap = productDAO.getAllProductsAsMap();
        Map<String,Product> barcodeToProduct = new HashMap<>(); //add a method to get the map from a DAO

        String previousLine="";
        String previousMaster="";
        String newline="";
        ArrayList<String>barcodes = new ArrayList<>();
        StoreBasedAttributes sba = new StoreBasedAttributes();
        StoreBasedAttributes previousSba = new StoreBasedAttributes();

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                String master = newline.substring(0, 11).trim();
                String hope = newline.substring(11,18).trim();
                String description = newline.substring(18, 53);
                String dept = newline.substring(92,95).trim();
                String barcode = newline.substring(79,92).trim();

                if(master.compareTo(previousMaster)==0) {
                    barcodes.add(barcode);
                } else {
                    sba = new StoreBasedAttributes();
                    sba.setDescription(description);
                    sba.setDepartment(dept);
                    sba.setMasterCode(master);
                    sba.setHope(hope);

                    findProduct(barcodeToProduct,barcodes,previousSba,store);
                    /*
                    after the previous function we will have either updated
                    an existing product or added a new on to the map
                     */




                    previousSba = sba;
                }


                previousLine = newline;
                previousMaster = master;


                //String kouta = line.substring(53, 60);
                //String fpaCode = line.substring(60, 61);
                //String costPrice = line.substring(61,70);
                //String salePrice = line.substring(70,78);
                //String availability = line.substring(78, 79);

                //String unit = line.substring(95,96);
                //String status = line.substring(96,97);
                }
            } catch (IOException ex) {
            throw new RuntimeException(ex);
        //System.out.println("saving hopeCodes");
        //productDAO.updateProducts(productMap.values().stream().toList());
        //System.out.println("hopeCodes saved");
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
        }


        ProductDAO productDAO = new ProductDAO();
        Map<String,Product> productMap = productDAO.getAllProductsAsMap();

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            String line ;
            while (((line = br.readLine()) != null) && (line.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                String master = line.substring(0, 11);
                String hope = line.substring(11,18).trim();
                String dept = line.substring(92,95);
                //String disc = line.substring(18, 53);
                //String kouta = line.substring(53, 60);
                //String fpaCode = line.substring(60, 61);
                //String costPrice = line.substring(61,70);
                //String salePrice = line.substring(70,78);
                //String availability = line.substring(78, 79);
                //String barcode = line.substring(79,92);
                //String unit = line.substring(95,96);
                //String status = line.substring(96,97);
                if(productMap.get(master)!=null){
                    sba = new StoreBasedAttributes();
                    sba.setStore(store);
                    sba.setHope(hope);
                    sba.setDepartment(dept);
                    boolean exists = false;
                    if(!productMap.get(master).getStoreBasedAttributes().isEmpty()){
                        System.out.println("^ ^ ^");
                        for(int j=0;j<productMap.get(master).getStoreBasedAttributes().size();j++){
                            if(productMap.get(master).getStoreBasedAttributes().get(j).getStore().compareTo(sba.getStore())==0){
                                productMap.get(master).getStoreBasedAttributes().get(j).setHope(sba.getHope());
                                productMap.get(master).getStoreBasedAttributes().get(j).setFamily(sba.getFamily());
                                productMap.get(master).getStoreBasedAttributes().get(j).setDepartment(sba.getDepartment());
                                exists = true;
                                break;
                            }
                        }
                    }
                    if(!exists){
                        productMap.get(master).getStoreBasedAttributes().add(sba);
                    }
                }
            }
            System.out.println("saving hopeCodes");
            productDAO.updateProducts(productMap.values().stream().toList());
            System.out.println("hopeCodes saved");
        } catch (IOException e) {
            System.out.println("something failed");
            throw new RuntimeException(e);
        }
    }

    private void findProduct(Map<String,Product> barToMap,List<String> barcodes,StoreBasedAttributes sba,StoreNames store) {
        List<Product> matchingProducts = new ArrayList<>();
        int matches = 0;
        for(String barcode:barcodes){
            if(barToMap.get(barcode)!=null){
                matches++;
                if(!matchingProducts.contains(barToMap.get(barcode))){
                    matchingProducts.add(barToMap.get(barcode));
                }
            }
        }
        if(matchingProducts.size()>1){
            //conflict
            return;
        }

        if(matchingProducts.size()==1&&barcodes.size()>1&&matches==1){
            //possible conflict
            return;
        }
        if(matchingProducts.size()==1&&barcodes.size()==1){
            Product matchingProduct = matchingProducts.get(0);
            String matchingDepartment = matchingProduct.getDepartment();
            if(Integer.parseInt(matchingDepartment.trim())<0){
                //error
                return;
            }
            if(matchingDepartment.compareTo(sba.getDepartment())==0){
                //inferred
                return;
            }
            if(matchingDepartment.compareTo(sba.getDepartment())!=0){
                //conflict
                return;
            }
            System.out.println("- - - - \n" +
                    "shouldn't reach this part of the code\n" +
                    "something wrong with handling cases of depts\n" +
                    "- - - - ");
            return;
        }
        if(matchingProducts.size()==1&&matches>1){
            //safe
            return;
        }
        if(matchingProducts.isEmpty()){
            //new Product
            return;
        }
        System.out.println("- - - - - \n" +
                "something is wrong with finding the product \n" +
                "a case has slept "+sba.getDescription()+" @ "+store.getName()+"\n" +
                "- - - - ");
    }


}
