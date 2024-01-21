package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.Sinartiseis.Serialization;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.StoreNames;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javax.crypto.spec.PSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ImportTxtsView extends ChildController{
    public TableView<Product> tableBarcodes;
    private ObservableList<Product> obsProducts= FXCollections.observableArrayList();

    List<Product> toCreate = new ArrayList<>();
    List<Product> toUpdate = new ArrayList<>();
    List<PosEntry> posEntries = new ArrayList<>();
    Map<File, StoreNames> itemFiles = new HashMap<>();
    Map<File, StoreNames> posSaleFiles = new HashMap<>();
    private Map<String,Product> barcodeToProduct = new HashMap<>();

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

        readBars();
        readFiles();











        testAddRandomProducts();
        saveProductList();
        //testLoadProductsFromDatabase();

        //editSomeProducts();
        //createRandomSbas();
        //loadSbas();

    }

    private void readFiles(){
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
                            if(store.compareTo(StoreNames.PERISTERI)==0){
                                System.out.println("- - - here starts the chaos - - - ");
                                extractItemsLines(txt,StoreNames.PERISTERI);
                            }
                            System.out.println("processing "+txt.getName()+" for store "+store);
                        });
                        posTxts.forEach(txt->{
                            System.out.println("processing "+txt.getName()+" for store "+store);
                        });
                    } else {
                        System.out.println("- - - - not all type of files found for store "+store);
                    }
                });
    }
    private void saveProductList(){
        ProductDAO productDAO = new ProductDAO();
        List<Product> newProducts = new ArrayList<>();
        List<Product> updateProducts = new ArrayList<>();

        List<Product> allProducts =new ArrayList<>();
        barcodeToProduct.values().forEach(product->{
            //System.out.println("all products "+product.getInvDescription());
            if(!allProducts.contains(product)){
                System.out.println("is not caontained");
                allProducts.add(product);
            } else {
                System.out.println("is contained");
            }
        });
        System.out.println("all products are "+allProducts.size());
        allProducts.forEach(product -> {
            if(product.getId()!=null){
                updateProducts.add(product);
            } else {
                newProducts.add(product);
            }
        });
        System.out.println("to update products are "+updateProducts.size());
        System.out.println("to create products are "+newProducts.size());
        productDAO.updateProducts(updateProducts);
        productDAO.saveProducts(newProducts);
    }
    private void initTableBarcodes(){
        TableColumn<Product,String> barcodeCol = new TableColumn<>("barcodes");
        barcodeCol.setCellValueFactory(cellData->{
            AtomicReference<String> text = new AtomicReference<>("");
            cellData.getValue().getBarcodes().forEach(barcode->{
                text.set(text + barcode + "\n");
            });
            return new ReadOnlyStringWrapper(text.get());
        });

        TableColumn<Product,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });

        tableBarcodes.getColumns().setAll(barcodeCol,descriptionCol);
        tableBarcodes.setItems(obsProducts);
    }

    private void loadSbas(){
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> allSbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();

        allSbas.forEach(sba->{
            if(sba.getProduct()!=null){
                System.out.println("not null sbas product "+sba.getProduct().getInvDescription());
            }
        });

        System.out.println("all the sbas are "+allSbas.size());

        List<StoreBasedAttributes> emptySbas = storeBasedAttributesDAO.getStoreBasedAttributesWithNoProduct();
        System.out.println("confilcted sbas are "+emptySbas.size());
    }

    private void readBars(){
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.getAllProducts();
        products.forEach(product->{
            System.out.println("product "+product.getInvDescription()+" size "+product.getAttributes().size());
            product.getAttributes().values().forEach(sba->{
                sba.getBarcodes().forEach(barcode->{
                    System.out.println("analysis "+barcode+" for "+product.getInvDescription());
                });
            });
        });

        initTableBarcodes();

        barcodeToProduct.clear();
        products.forEach(product -> {
            product.getBarcodes().forEach(barcode->{
                System.out.println("adding "+product.getInvDescription()+" to the map");
                if(barcodeToProduct.get(barcode)==null){
                    barcodeToProduct.put(barcode,product);
                } else {
                    System.out.println(" a duplicate barcode is found");
                }
            });
        });
        System.out.println("the barcodes are "+barcodeToProduct.size());
        if(barcodeToProduct.size()!=0)
        barcodeToProduct.get(barcodeToProduct.keySet().stream().toList().get(0)).getBarcodes().forEach(bar->{
            System.out.println("the barcode of the first product is "+bar);
        });
        obsProducts.setAll(products);
    }

    private void createRandomSbas() {
        ProductDAO productDAO = new ProductDAO();
        Product product = new Product();
        product.setInvDescription("a random description");
        productDAO.saveProduct(product); // Save and persist the product

        List<StoreBasedAttributes> sbas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StoreBasedAttributes sba = new StoreBasedAttributes();
            sba.setHope("0100202");
            sba.setMasterCode("010101010");
            sba.setDepartment("12");
            sba.setStore(StoreNames.PERISTERI);
            if (i == 0) {
                sba.setProduct(product); // Set product only after it's persisted
            }
            sbas.add(sba);
        }

        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.saveSBAs(sbas); // Save store-based attributes
    }
    private void editSomeProducts(){
        ProductDAO productDAO = new ProductDAO();
        List<Product> allProducts = productDAO.getAllProducts();

        for(int i=0;i<allProducts.size();i=i+2){
            allProducts.get(i).setInvDescription(" 1 edited inv description "+i);
            allProducts.get(i).getAttributes().values().stream().toList().get(0).setDescription(" 1 edited new description "+i);
        }

        productDAO.updateProducts(allProducts);
        List<Product> editProducts = productDAO.getAllProducts();

        editProducts.forEach(product->{
            System.out.println(" :: "+product.getInvDescription());
            System.out.println(" -- "+product.getAttributes().values().stream().toList().get(0).getDescription());
        });

    }

    private void testLoadProductsFromDatabase(){
        System.out.println("the loading started");
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.getAllProducts();
        System.out.println("the list is the "+products.size());
        AtomicInteger i = new AtomicInteger(0);
        products.forEach(product->{
            i.getAndIncrement();
            System.out.println("the "+i.get()+" product description is "+product.getInvDescription());
            product.getAttributes().values().forEach(sba->{
                System.out.println(" - "+sba.getStore().getName()+" | "+sba.getDescription()+" "+sba.getFamily());
            });
            product.getBarcodes().forEach(barcode->{
                System.out.println(":: "+barcode+" :: "+product.getInvDescription());
            });
        });

        Map<String,Product> barcodeToProduct = new HashMap<>();
        products.forEach(product->{
            product.getBarcodes().forEach(barcode->{
                if(barcodeToProduct.get(barcode)==null){
                    barcodeToProduct.put(barcode,product);
                } else {
                    System.out.println("there is an error - we have a double barcode");
                }
            });
        });
        System.out.println("the size of the product map "+barcodeToProduct.size());
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
        entriesFileDAO.saveEntriesFile( new EntriesFile(itemFile.getPath(), fChecksum ));
    }
    public void extractItemsLines(File file,StoreNames store){
        //Map<String,Product> productMap = productDAO.getAllProductsAsMap();
        //Map<String,Product> barcodeToProduct = new HashMap<>(); //add a method to get the map from a DAO

        List<String> allLines = new ArrayList<>();
        String newline="";
        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                allLines.add(newline);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
        }

        String previousMaster = "";
        StoreBasedAttributes sba;
        StoreBasedAttributes previousSba = null;
        for(int i=0;i<allLines.size();i++){
            String master = allLines.get(i).substring(0, 11).trim();
            String hope = allLines.get(i).substring(11,18).trim();
            String description = allLines.get(i).substring(18, 53).trim();
            String dept = allLines.get(i).substring(92,95).trim();
            String barcode = allLines.get(i).substring(79,92).trim();

            System.out.println("** reading a line ** "+description);

            if(i!=allLines.size()-1){
                //if it is not the last line
                if(master.compareTo(previousMaster)==0) {
                    //if it is the same mastercode as the last
                    if(previousSba!=null){
                        previousSba.getBarcodes().add(barcode);
                    } else {
                        System.out.println("error with null sba at items.txt reading");
                    }
                } else {
                    //if the mastercode changed
                    sba = new StoreBasedAttributes();
                    sba.setDescription(description);
                    sba.setDepartment(dept);
                    sba.setMasterCode(master);
                    sba.setHope(hope);
                    sba.getBarcodes().add(barcode);
                    sba.setStore(store);


                    if(previousSba!=null){
                        //we save the previous sba
                        System.out.println("*!* previous master changed and not last line*!*");
                        findProduct(previousSba);
                    }

                    previousSba = sba;
                }
                previousMaster = master;
            } else {
                //if it is the last line
                if (previousMaster.compareTo(master)==0){
                    //if the previous master is the same as the current
                    if(previousSba!=null){
                        //save the previous sba
                        previousSba.getBarcodes().add(barcode);
                        System.out.println("*!* last line previous master same with current *!*");
                        findProduct(previousSba);
                    } else {
                        System.out.println("strange");
                    }
                } else {
                    //if the last master is different from the previous
                    //save the previous sba
                    System.out.println("*!* last line - master changed *!*");
                    findProduct(previousSba);
                    //create new sba
                    sba = new StoreBasedAttributes();
                    sba.setDescription(description);
                    sba.setDepartment(dept);
                    sba.setMasterCode(master);
                    sba.setHope(hope);
                    sba.getBarcodes().add(barcode);
                    sba.setStore(store);
                    System.out.println("*!* final part *!*");
                    findProduct(sba);
                }
            }


        }
    }
    private void testAddRandomProducts(){
        List<Product> products = new ArrayList<>();
        for(int i=0;i<10;i++){
            Product product = new Product();
            product.setInvDescription("inv description "+i);
            StoreBasedAttributes sba = new StoreBasedAttributes();
            sba.setProduct(product);
            sba.setStore(StoreNames.PERISTERI);
            sba.setDescription("sba description "+i+"_");
            sba.setHope("1201234");
            sba.setDepartment("12");
            sba.setMasterCode("010101010");

            for (int j=0;j<2;j++){
                sba.getBarcodes().add("01020304"+i+""+j);
            }
            product.getAttributes().put(sba.getStore(),sba);
            products.add(product);
        }
        ProductDAO productDAO = new ProductDAO();
        List<Product> newProducts = new ArrayList<>();
        List<Product> updateProducts = new ArrayList<>();
        products.forEach(product -> {
            if(product.getId()!=null){
                updateProducts.add(product);
            } else {
                newProducts.add(product);
            }
        });
        productDAO.updateProducts(updateProducts);
        productDAO.saveProducts(newProducts);
    }

    /*
    check all the results from
     */

    private void findProduct(StoreBasedAttributes sba) {
        //here we have to find the product that the sba is to be assigned
        //if we have to create a new product we update the map
        List<Product> matchingProducts = new ArrayList<>();
        int matches = 0;

        for(String barcode:sba.getBarcodes()){
            if(barcodeToProduct.get(barcode)!=null){
                matches++;
                if(!matchingProducts.contains(barcodeToProduct.get(barcode))){
                    matchingProducts.add(barcodeToProduct.get(barcode));
                }
            }
        }

        if(matches==0){

            Product product = new Product();
            product.setInvDescription(sba.getDescription());
            product.addSba(sba);
            System.out.println("-- the matches are 0 -- bars :"+sba.getBarcodes().size());

            for(String bar:sba.getBarcodes()){
                System.out.println("bar "+bar+" product "+product.getInvDescription());
                barcodeToProduct.put(bar,product);
            }
        } else if(matches==1){
            System.out.println("-- the matches are 1 --");
            if(!matchingProducts.isEmpty()){
                if(matchingProducts.get(0).getFamilies().contains(sba.getFamily())){
                    if(matchingProducts.get(0).hasConflict()){
                        //if there is an already existing conflict
                        //mark the new sba as in conflict
                        sba.setHasConflict(true);
                    }
                    matchingProducts.get(0).addSba(sba);

                    for(String bar:sba.getBarcodes()){
                        barcodeToProduct.put(bar,matchingProducts.get(0));
                    }
                } else {
                    //the matching product is on a different family than the new sba
                    sba.setHasConflict(true);
                    matchingProducts.get(0).markSBAsInConflict();
                    for(String bar:sba.getBarcodes()){
                        barcodeToProduct.put(bar,matchingProducts.get(0));
                    }
                    //CONFLICT
                }
            }
        } else if(matches>1){
            System.out.println("-- the matches are greater than 1 --");
            if(matchingProducts.size()==1){
                if(matchingProducts.get(0).hasConflict()){
                    sba.setHasConflict(true);
                }
                matchingProducts.get(0).addSba(sba);
                for(String bar:sba.getBarcodes()){
                    barcodeToProduct.put(bar,matchingProducts.get(0));
                }
            } else if (matchingProducts.size()>1){
                System.out.println("-- the matches are less than 1 --");
                //here barcodes from an sba return multiple products
                //this is a sure conflict, so we  take all the sba with are conflicting
                //and we mark them as conflicting
                sba.getBarcodes().forEach(barcode->{
                    if(barcodeToProduct.get(barcode)!=null){
                        if( barcodeToProduct.get(barcode).getSbaFromBarcode(barcode)!=null){
                            barcodeToProduct.get(barcode).getSbaFromBarcode(barcode).forEach(sba2->{
                                sba2.setHasConflict(true);
                            });
                        }
                    }
                });
                sba.setHasConflict(true);
                for(String bar:sba.getBarcodes()){
                    barcodeToProduct.put(bar,matchingProducts.get(0));
                }
                //CONFLICT
            } else {
                System.out.println("mystery");
            }
        }

    }
}

//String kouta = line.substring(53, 60);
//String fpaCode = line.substring(60, 61);
//String costPrice = line.substring(61,70);
//String salePrice = line.substring(70,78);
//String availability = line.substring(78, 79);

//String unit = line.substring(95,96);
//String status = line.substring(96,97);
