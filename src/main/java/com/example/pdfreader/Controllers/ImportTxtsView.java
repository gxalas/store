package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductWithAttributes;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderListener;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.Sinartiseis.Serialization;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.apache.commons.collections4.SplitMapUtils;

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
    public TextField txtfProducts = new TextField();
    public Button btnAdd;
    public Button btnMatch;
    public Button btnCalcPos;
    public Button btnLoadTxt;
    public Text txtPosErrors ;
    private ObservableList<StoreBasedAttributes> obsAllSbas = FXCollections.observableArrayList();
    private ObservableList<Product> obsAllProducts = FXCollections.observableArrayList();
    public TableView<StoreBasedAttributes> tableSbas;
    private ObservableList<StoreBasedAttributes> obsAllSbasTable = FXCollections.observableArrayList();
    public TableView<Product> tableProducts = new TableView<>();
    public TableView<StoreBasedAttributes> tableFilteredSbas = new TableView<>();
    private ObservableList<StoreBasedAttributes> obsFilteredSbas = FXCollections.observableArrayList();
    private ObservableList<Product> obsProductsTable = FXCollections.observableArrayList();


    //second page


    public ComboBox<String> cbStores = new ComboBox<>();
    public TableView<StoreBasedAttributes> tableSbaConflicts = new TableView<>();

    private ObservableList<StoreBasedAttributes> obsSbaConflicts = FXCollections.observableArrayList();
    public TableView<Product> tableMatchingProducts = new TableView<>();
    private ObservableList<Product> obsMatchingProducts = FXCollections.observableArrayList();
    public TableView<StoreBasedAttributes> tableMatchingSbas = new TableView<>();
    private ObservableList<StoreBasedAttributes> obsMatchingSbas = FXCollections.observableArrayList();
    private Map<String,ProductWithAttributes> barcodeToProductWithAttributes = new HashMap<>();
    private Map<File, StoreNames> itemFiles = new HashMap<>();
    private Map<File, StoreNames> posSaleFiles = new HashMap<>();
    private Map<StoreNames,Map<String,StoreBasedAttributes>> sbaMegaMap= new HashMap<>();
    private Map<StoreNames,Map<Date,Map<String,PosEntry>>> megaPosMap = new HashMap<>();
    private List<String> hardConflicts = new ArrayList<>(); // we found more than one product for a sba
    private List<String> softConflicts = new ArrayList<>(); // the product found has not a department as the sba
    private ListChangeListener<Product>  btnVisibilityListener = change -> setBtnVisibility();
    private ListChangeListener<StoreBasedAttributes> allSbasListener = change -> {
        obsAllSbasTable.setAll(obsAllSbas);
        refreshTableConflicts();
    };
    private ListChangeListener<Product> allProductsListener = change -> {
        List<Product> someProducts = new ArrayList<>();
        for(int i =0;i<20;i++){
            someProducts.add(obsAllProducts.get(i));
        }
        Platform.runLater(()->{
            obsProductsTable.setAll(someProducts);
        });

    };
    private ChangeListener<Product> filteringSbas = (observableValue, product, t1) -> {
        if(t1!=null){
            List<StoreBasedAttributes> filtered = new ArrayList<>();
            obsAllSbas.forEach(sba->{
                if(sba.getProduct()!=null){
                    if (sba.getProduct().getInvDescription().compareTo(t1.getInvDescription())==0){
                        filtered.add(sba);
                    }
                }
            });
            obsFilteredSbas.setAll(filtered);
        }
    };

    @Override
    public void initialize(HelloController hc) {

        System.out.println("importing text is here");
        this.parentDelegate = hc;
        readingTxtFilesLogic();
        txtfProducts.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(txtfProducts.getText().trim().compareTo("")!=0){
                    List<Product> filteredProducts = new ArrayList<>();
                    obsAllProducts.forEach(product->{
                        if(product.getInvDescription().toLowerCase().contains(txtfProducts.getText().toLowerCase())){
                            filteredProducts.add(product);
                        }
                    });
                    obsProductsTable.setAll(filteredProducts);
                } else {
                    obsProductsTable.setAll(obsAllProducts);
                }
            }
        });

    }


    @Override
    public void addMyListeners() {

        obsMatchingProducts.addListener(btnVisibilityListener);
        obsAllSbas.addListener(allSbasListener);
        obsAllProducts.addListener(allProductsListener);
        tableProducts.getSelectionModel().selectedItemProperty().addListener(filteringSbas);
    }

    @Override
    public void removeListeners(HelloController hc) {

        obsMatchingProducts.removeListener(btnVisibilityListener);
        obsAllSbas.removeListener(allSbasListener);
        obsAllProducts.removeListener(allProductsListener);
        tableProducts.getSelectionModel().selectedItemProperty().removeListener(filteringSbas);


        megaPosMap.clear();
        sbaMegaMap.clear();
        barcodeToProductWithAttributes.clear();
        obsProductsTable.clear();
        obsMatchingProducts.clear();
        obsAllSbas.clear();
        obsSbaConflicts.clear();
        obsMatchingSbas.clear();
        obsAllProducts.clear();
        obsAllSbasTable.clear();
        System.out.println("here we are");
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


        initTables();
        initButtons();
        loadContent();
        /*

        */


        System.out.println("- - - - -  logic ending - - - ");
    }

    private void savePosEntries(){
        if(megaPosMap.isEmpty()){
            return;
        }
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        List<PosEntry> allPosEntries = posEntryDAO.getAllPosEntries();
        System.out.println("the pos entries at the database are "+allPosEntries.size());

        Map<StoreNames,Map<Date,Map<String,PosEntry>>> dbPosMap = new HashMap<>();
        allPosEntries.forEach(posEntry -> {
            dbPosMap.computeIfAbsent(posEntry.getStoreName(), k -> new HashMap<>());
            dbPosMap.get(posEntry.storeName).computeIfAbsent(posEntry.getDate(),k->new HashMap<>());
            if(dbPosMap.get(posEntry.storeName).get(posEntry.getDate()).get(posEntry.getMaster())!=null){
                System.err.println("\n\n a pos entry is double on the map \n\n");
            }
            dbPosMap.get(posEntry.storeName).get(posEntry.getDate()).putIfAbsent(posEntry.getMaster(), posEntry);
        });

        List<PosEntry> toSave = new ArrayList<>();
        megaPosMap.keySet().forEach(store->{
            megaPosMap.get(store).keySet().forEach(date->{
                megaPosMap.get(store).get(date).values().forEach(pos->{
                    if(dbPosMap.get(store)==null){
                        toSave.add(pos);
                    } else if (dbPosMap.get(store).get(date)==null){
                        toSave.add(pos);
                    } else if(dbPosMap.get(store).get(date).get(pos.getMaster())==null){
                        //should save
                        if(!toSave.contains(pos)){
                            toSave.add(pos);
                        } else {
                            System.err.println("the pos already exists in the to save list");
                        }

                    } else {
                        //should check if they are the same
                    }
                });
            });
        });
        System.out.println("we are attempting to save the pos entries "+toSave.size());

        posEntryDAO = new PosEntryDAO();
        posEntryDAO.savePosEntries(toSave);
    }

    /**
    keep this, we may need this when we change the rest of the program
     */
    private void loadProductWithAttributes(){
        List<ProductWithAttributes> products = ProductWithAttributes.getAllProductsWithAttributes(HibernateUtil.getEntityManagerFactory().createEntityManager());
        products.forEach(product->{
            System.out.println("the product "+product.getProduct().getInvDescription()+" has "+product.getAttributes().size()+" attributes");
        });
    }
    private void readFiles(){
        itemFiles.forEach(this::createTheMegaSbaMap);
        posSaleFiles.forEach(this::createTheMegaPosMap);
    }
    private Set<StoreBasedAttributes> getProductsToPosAndSbas(){
        List<Product> newProducts = new ArrayList<>();
        List<String> nullMasters = new ArrayList<>(); // no sba found with the pos master -> probably item file older than pos file
        Set<StoreBasedAttributes> sbasToSave = new HashSet<>();

        megaPosMap.keySet().forEach(store->{
            megaPosMap.get(store).keySet().forEach(date->{
                for (String master : megaPosMap.get(store).get(date).keySet()) {
                    StoreBasedAttributes sba = sbaMegaMap.get(store).get(master);
                    if (sba == null) {
                        // here i think we cant find a store based attribute for this product on the store
                        // probably because the item file is older that the pos file
                        String k = master+" | | "+store+" | | "+megaPosMap.get(store).get(date).get(master).getDescription()+" | | ";
                        if(!nullMasters.contains(k)){
                            nullMasters.add(k);
                        }
                        continue;
                    }
                    if (sba.getProduct() == null) {
                        findProduct(sba, newProducts);
                    }
                    if (sba.getProduct() == null) {
                        //System.out.println("couldn't find a product");
                    }
                    megaPosMap.get(store).get(date).get(master).setProduct(sba.getProduct());
                    sbasToSave.add(sba);
                }
            });
        });
        System.out.println("\n the null masters are "+nullMasters.size());
        return sbasToSave;
    }
    private void createTheMegaSbaMap(File file, StoreNames store){
        String newline = "";
        sbaMegaMap.computeIfAbsent(store, k -> new HashMap<>());
        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo
                StoreBasedAttributes currentSba = sbaFromLine(newline);
                currentSba.setStore(store);

                if(sbaMegaMap.get(store).get(currentSba.getMasterCode())==null){
                    //if current sba is not at the map
                    sbaMegaMap.get(store).put(currentSba.getMasterCode(),currentSba);
                } else {
                    //current sba is on the map -> update
                    updateSba(sbaMegaMap.get(store).get(currentSba.getMasterCode()),currentSba);
                }

            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
        }
    }
    private void updateSba(StoreBasedAttributes oldSba,StoreBasedAttributes newSba){
        if(oldSba.getDepartment().compareTo(newSba.getDepartment())!=0){
            oldSba.setDepartment(newSba.getDepartment());
            System.out.println("the department got updated for "+oldSba.getMasterCode());
        }
        if(oldSba.getHope().compareTo(newSba.getHope())!=0){
            oldSba.setHope(newSba.getHope());
            System.out.println("the hope changed for "+oldSba.getMasterCode());
        }

        if(oldSba.getDescription().compareTo(newSba.getDescription())!=0){
            oldSba.setDescription(newSba.getDescription());
            System.out.println("the department changed for "+oldSba.getMasterCode());
        }

        newSba.getBarcodes().forEach(bar->{
            if(!oldSba.getBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
        newSba.getConflictingBarcodes().forEach(bar->{
            if(!oldSba.getConflictingBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getConflictingBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
    }
    public void getFilesFromFolder(){
        File parentFolder = SySettings.txtFolderPath.toFile();
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
    private void createTheMegaPosMap(File file, StoreNames storeName){
        System.out.println("- processing : "+file.getName()+", for store : "+storeName.getName());
        String fChecksum = TextExtractions.calculateChecksum(file);
        if(parentDelegate.listManager.getFileChecksums().contains(fChecksum)){
            System.out.println("The file: " + file.getName() + " already exists "+fChecksum);
            return;
        }

        megaPosMap.computeIfAbsent(storeName, k -> new HashMap<>());
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
                            index = 0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;
                        megaPosMap.get(storeName).computeIfAbsent(temp.getDate(), k -> new HashMap<>());
                        megaPosMap.get(storeName).get(temp.getDate()).putIfAbsent(temp.getMaster(), temp);
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
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(file.getPath(), fChecksum));
    }
    private StoreBasedAttributes sbaFromLine(String line){
        StoreBasedAttributes sba = new StoreBasedAttributes();
        String master = line.substring(0, 11).trim();
        String hope = line.substring(11,18).trim();
        String description = line.substring(18, 53).trim();
        String dept = line.substring(92,95).trim();
        String barcode = line.substring(79,92).trim();
        sba.setDescription(description);
        sba.setMasterCode(master);
        sba.setHope(hope);
        sba.setDepartment(dept);

        if(sba.getHope().length()>6){
            if((barcode.startsWith(sba.getHope().substring(0,6)))&&(sba.getFamily().compareTo("930")==0)){
                sba.getConflictingBarcodes().add(barcode);
            } else {
                sba.getBarcodes().add(barcode);
            }
        }
        return sba;
    }
    private void findProduct(StoreBasedAttributes sba,List<Product> toSaveProducts) {
        Set<ProductWithAttributes> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode->{
            if(barcodeToProductWithAttributes.get(barcode)!=null){
                matchingProducts.add(barcodeToProductWithAttributes.get(barcode));
            }
        });
        /*
        - - null master->
        we cannot do anything, it must be loaded a new items file,
        what is the cost of this error??
        we are having a posEntry without having a product
        we can create a product from a custom Pos Entry : product.attributes.put(customStore,sba)


        - - softConflict ->
        we have to "accept" the possible conflict

        - - hardConflicting ->
        we have to remove the conflicting barcodes from the wrong item



        ##
        so, we have to flag the kind of error that its sba is facing

        * in case of a soft conflict we can add the matching products in an attribute
        of the sba (eg. possible products)

        * in case of hard conflict we have to be able to view :
        the SBA without a product assigned
        the possible products that are more than 1 (in case of hard conflicts)
        */

        if(matchingProducts.isEmpty()){
            Product product = new Product();
            product.setInvDescription(sba.getDescription());
            sba.setProduct(product);
            ProductWithAttributes tempPWA = new ProductWithAttributes(product,sba);
            sba.getBarcodes().forEach(barcode->{
                if(!barcode.startsWith(sba.getHope().trim())){
                    barcodeToProductWithAttributes.put(barcode,tempPWA);
                }
            });
            toSaveProducts.add(product);
        } else if(matchingProducts.size()==1){
            Set<String> matchingDeparments = new HashSet<>();

            matchingProducts.stream().toList().get(0).getAttributes().values().forEach(mSba->{
                matchingDeparments.add(mSba.getDepartment());
            });
            if (matchingDeparments.contains(sba.getDepartment())){
                sba.setProduct(matchingProducts.stream().toList().get(0).getProduct());
            } else {
                String softLog = "probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription();
                if(!softConflicts.contains(softLog)){
                    softConflicts.add(softLog);
                }
                if(false){
                    // these conflict have to be
                    // the matching products do not have the same department with the sba
                    // we have to add manually the product to the sba, and then probably the departments will much
                    System.out.println("probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription());
                }
            }
        } else {
            //here are the conflicts that we have more than one matching products
            //this is caused because one store has two products properly
            //and then the other store has only one of these products
            //and for some reason that product has both of the barcodes
            // - - - - - - - - - Handling
            //in the product, we go to the attribute "conflicting barcodes", and we add to the
            //list the barcode of the wrong product - that will be ignored - and put it to the
            //map of the store: product.get(store).put(CONFLICTING_BARCODE);
            String hardLog = "hard conflict : "+sba.getMasterCode()+" , "+sba.getStore()+" , "+sba.getDescription();
            if(!hardConflicts.contains(hardLog)){
                hardConflicts.add(hardLog);
            }

        }
    }
    private void loadContent(){
        MyTask loadAllSbas = new MyTask(()->{
            StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
            obsAllSbas.setAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());
            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading all sbs",
                "getting all the store based attributes from the database",
                loadAllSbas);

        MyTask loadAllProducts = new MyTask(()->{
            ProductDAO productDAO = new ProductDAO();
            obsAllProducts.setAll(productDAO.getAllProducts());
            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading products",
                "getting all products from the database",
                loadAllProducts
        );



    }
    private void setBtnVisibility(){
        btnAdd.setVisible(tableMatchingProducts.getItems().size() == 1);
        btnMatch.setVisible(!tableMatchingProducts.getItems().isEmpty() && tableMatchingProducts.getSelectionModel().getSelectedItem() != null);
    }
    private void saveStoreBasedAttributes(List<StoreBasedAttributes> toSave){
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();

        List<StoreBasedAttributes> dbSbas = new ArrayList<>();
        dbSbas.addAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());

        Map<StoreNames,Map<String,StoreBasedAttributes>> dbSbaMap = new HashMap<>();
        dbSbas.forEach(sba->{
            dbSbaMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            dbSbaMap.get(sba.getStore()).put(sba.getMasterCode(),sba);
        });
        dbSbas.clear();
        dbSbas = null;

        List<StoreBasedAttributes> saveList=  new ArrayList<>();
        toSave.forEach(sba->{
            if(dbSbaMap.get(sba.getStore())!=null){
                if(dbSbaMap.get(sba.getStore()).get(sba.getMasterCode())!=null){
                    if(dbSbaMap.get(sba.getStore()).get(sba.getMasterCode()).areEqual(sba)){
                        //do not save
                    } else {
                        //there is an error
                    }
                } else {
                    saveList.add(sba);
                    //save
                }
            } else {
                saveList.add(sba);
            }
        });

        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.getAllProducts();
        Map<String,Product> mapProducts = new HashMap<>();

        products.forEach(product->{
            if(mapProducts.get(product.getInvmaster())!=null){
                System.out.println("not expected while creating product map");
            } else {
                mapProducts.put(product.getInvmaster(),product);
            }
        });

        AtomicInteger counter = new AtomicInteger();



        Set<Product> toSaveProducts = new HashSet<>();
        Set<Product> toUpdProducts = new HashSet<>();

        saveList.forEach(sba->{
            if(sba.getFamily().compareTo("930")!=0){
                if(mapProducts.get(sba.getMasterCode())!=null){
                    sba.setProduct(mapProducts.get(sba.getMasterCode()));
                    counter.getAndIncrement();
                    toUpdProducts.add(sba.getProduct());
                } else{
                    toSaveProducts.add(sba.getProduct());
                }
            }else{
                toSaveProducts.add(sba.getProduct());
            }
        });

        System.out.println("the moved products are "+counter);







        productDAO = new ProductDAO();

        if(!toSaveProducts.isEmpty()){
            try {
                productDAO.saveProducts(toSaveProducts.stream().toList());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        if(saveList.isEmpty()){
            return;
        }
        storeBasedAttributesDAO.saveSBAs(saveList);
    }
    private void initTables(){
        initTableSBA();
        initTableProducts();
        initTableConflicts();
        initTableMatchingProducts();
        initTableMatchingSbas();
        initTableFilteredSbas();
    }
    private void initButtons(){
        btnAdd.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(tableMatchingProducts.getItems().size()!=1){
                    System.err.println("more than one products on the table");
                    return;
                }
                Product product = tableMatchingProducts.getItems().get(0);
                if(tableSbaConflicts.getSelectionModel().getSelectedItem()==null){
                    System.err.println(" you have not selected a conflicting sba");
                    return;
                }
                StoreBasedAttributes sba = tableSbaConflicts.getSelectionModel().getSelectedItem();
                sba.setProduct(product);
                StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                List<StoreBasedAttributes> saveSba = new ArrayList<>();
                saveSba.add(sba);
                storeBasedAttributesDAO.updateStoreBasedAttributes(saveSba);
                System.out.println("the sba has been saved");

                MyTask loadAllSbas = new MyTask(()->{
                    obsAllSbas.setAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());
                    return null;
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "Reloading the Sbas",
                        "getting all the store based attributes from the database",
                        loadAllSbas);

            }
        });
        btnMatch.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(tableSbaConflicts.getSelectionModel().getSelectedItem()==null){
                    System.err.println("you have not selected a conflicting sba");
                    return;
                }
                if(tableMatchingProducts.getSelectionModel().getSelectedItem()==null){
                    System.err.println("You have not selected a Product to add");
                    return;
                }
                if (tableMatchingSbas.getItems().isEmpty()){
                    System.err.println("the selected products has no sbas to take");
                    return;
                }
                StoreBasedAttributes sba = tableSbaConflicts.getSelectionModel().getSelectedItem();
                Product product = tableMatchingProducts.getSelectionModel().getSelectedItem();
                List<String> keepBars = new ArrayList<>();
                List<String> confBars = new ArrayList<>();
                tableMatchingSbas.getItems().forEach(mSba->{
                    mSba.getBarcodes().forEach(bar->{
                        if(!keepBars.contains(bar)){
                            keepBars.add(bar);
                        }
                    });
                });
                sba.getBarcodes().forEach(bar->{
                    if(!keepBars.contains(bar)){
                        confBars.add(bar);
                    }
                });
                sba.getBarcodes().clear();
                sba.getBarcodes().addAll(keepBars);
                sba.getConflictingBarcodes().addAll(confBars);
                sba.setProduct(product);


                StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                List<StoreBasedAttributes> oldsbas = new ArrayList<>();
                oldsbas.add(sba);
                storeBasedAttributesDAO.updateStoreBasedAttributes(oldsbas);

                StoreBasedAttributesDAO storeBasedAttributesDAO2 = new StoreBasedAttributesDAO();
                MyTask loadAllSbas = new MyTask(()->{
                    obsAllSbas.setAll(storeBasedAttributesDAO2.getAllStoreBasedAttributes());
                    return null;
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "loading all sbs",
                        "getting all the store based attributes from the database",
                        loadAllSbas);


            }
        });
        btnCalcPos.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                MyTask updatePosEntries = new MyTask(()->{
                    AtomicInteger noProduct = new AtomicInteger(0);
                    AtomicInteger yesProduct = new AtomicInteger(0);

                    StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                    Map<StoreNames, Map<String,StoreBasedAttributes>> sbaMap = storeBasedAttributesDAO.getSbaMap();

                    PosEntryDAO posEntryDAO = new PosEntryDAO();
                    List<PosEntry> posEntries = posEntryDAO.getAllPosEntries();

                    List<PosEntry> toSavePos = new ArrayList<>();

                    System.out.println("the pos entries at the database are "+posEntries.size());
                    posEntries.forEach(pos->{
                        if(pos.getProduct()==null){
                            if(sbaMap.get(pos.getStoreName())!=null){
                                if(sbaMap.get(pos.getStoreName()).get(pos.getMaster())!=null){
                                    if(sbaMap.get(pos.storeName).get(pos.getMaster()).getProduct()!=null){
                                        pos.setProduct(sbaMap.get(pos.storeName).get(pos.getMaster()).getProduct());
                                        toSavePos.add(pos);
                                        yesProduct.getAndIncrement();
                                    } else {
                                        noProduct.getAndIncrement();
                                    }
                                } else {
                                    noProduct.getAndIncrement();
                                }
                            }else {
                                noProduct.getAndIncrement();
                            }
                        } else {
                            yesProduct.getAndIncrement();
                        }
                    });
                    posEntryDAO = new PosEntryDAO();
                    txtPosErrors.setText("the pos Entries with no product are "+noProduct.get());
                    System.out.println("the yes product is "+yesProduct.get());
                    System.out.println("The to save pos are "+toSavePos.size());
                    posEntryDAO.savePosEntries(toSavePos);
                    return null;
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "updating pos entries",
                        "trying to add product to pos entries that have no pos entries",
                        updatePosEntries
                );
            }
        });
        btnLoadTxt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MyTask readingFiles = new MyTask(()->{
                    parentDelegate.listManager.loadFileChecksums();
                    getFilesFromFolder();
                    readFiles();
                    Set<StoreBasedAttributes> sbasToSave = getProductsToPosAndSbas();
                    saveStoreBasedAttributes(sbasToSave.stream().toList());
                    savePosEntries();
                    return null;
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "reading item and text",
                        "reading the txt files",
                        readingFiles);

            }
        });

    }
    private void initTableSBA(){
        TableColumn<StoreBasedAttributes,String> barcodeCol = new TableColumn<>("barcodes");
        barcodeCol.setCellValueFactory(cellData->{
            AtomicReference<String> result = new AtomicReference<>("");
            cellData.getValue().getBarcodes().forEach(bar->{
                result.set(result + bar + "\n");
            });
            return new ReadOnlyStringWrapper(result.get());
        });

        TableColumn<StoreBasedAttributes,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String d = "";
            if(cellData.getValue().getProduct()!=null){
                d = cellData.getValue().getMasterCode();
            }
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String result = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(result);
        });

        TableColumn<StoreBasedAttributes,String> conBarsCol = new TableColumn<>("ConflictBarcodes");
        conBarsCol.setCellValueFactory(cellData->{
            StringBuilder r = new StringBuilder();
            cellData.getValue().getConflictingBarcodes().forEach(bar->{
                r.append(bar);
                r.append("\n");
            });
            return new ReadOnlyStringWrapper(r.toString());
        });
        TableColumn<StoreBasedAttributes,String> productCol = new TableColumn<>("assigned Product");
        productCol.setCellValueFactory(cellData->{
            String d ="";
            if(cellData.getValue().getProduct()!=null){
                d = cellData.getValue().getProduct().getInvDescription();
            }
            return new ReadOnlyStringWrapper(d);
        });

        tableSbas.getColumns().setAll(barcodeCol,storeCol,descriptionCol,masterCol,conBarsCol,productCol);
        tableSbas.setItems(obsAllSbasTable);


    }
    private void initTableProducts(){
        //table Products
        TableColumn<Product,String> productDescCol = new TableColumn<>("description");
        productDescCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<Product,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String i = cellData.getValue().getInvmaster();
            return new ReadOnlyStringWrapper(i);
        });
        TableColumn<Product,String> codeCol = new TableColumn<>("code");
        codeCol.setCellValueFactory(cellData->{
            String c = cellData.getValue().getCode();
            return new ReadOnlyStringWrapper(c);
        });

        tableProducts.getColumns().setAll(productDescCol,masterCol,codeCol);
        tableProducts.setItems(obsProductsTable);

    }
    private void initTableConflicts(){
        TableColumn<StoreBasedAttributes,String> descCol = new TableColumn<>("description");
        descCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String s = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(s);
        });

        TableColumn<StoreBasedAttributes,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String m = cellData.getValue().getMasterCode();
            return new ReadOnlyStringWrapper(m);
        });

        TableColumn<StoreBasedAttributes,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            String h = cellData.getValue().getHope();
            return new ReadOnlyStringWrapper(h);
        });

        TableColumn<StoreBasedAttributes,String> familyCol = new TableColumn<>("family");
        familyCol.setCellValueFactory(cellData->{
            String f = cellData.getValue().getFamily();
            return new ReadOnlyStringWrapper(f);
        });

        TableColumn<StoreBasedAttributes,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDepartment();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> barcodesCol = new TableColumn<>("barcodes");
        barcodesCol.setCellValueFactory(cellData->{
            StringBuilder sb = new StringBuilder();

            cellData.getValue().getBarcodes().forEach(bar->{
                sb.append(bar).append("\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<StoreBasedAttributes,String> productCol = new TableColumn<>("product");
        productCol.setCellValueFactory(cellData->{
            String p = "";
            if(cellData.getValue().getProduct()==null){
                p="0";
            } else {
                p="1";
            }
            return new ReadOnlyStringWrapper(p);
        });

        tableSbaConflicts.getColumns().setAll(descCol,storeCol,masterCol,hopeCol,familyCol,departmentCol,barcodesCol,productCol);
        tableSbaConflicts.setItems(obsSbaConflicts);
        List<String> cbStoreValues = new ArrayList<>();
        cbStoreValues.add("All");
        Arrays.stream(StoreNames.values()).toList().forEach(val->{
            if(val.compareTo(StoreNames.NONE)!=0 && val.compareTo(StoreNames.ALL)!=0){
                cbStoreValues.add(val.getName());
            }
        });
        cbStores.setItems(FXCollections.observableList(cbStoreValues));
        cbStores.getSelectionModel().select(0);
        cbStores.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                refreshTableConflicts();
            }
        });

        tableSbaConflicts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<StoreBasedAttributes>() {
            @Override
            public void changed(ObservableValue<? extends StoreBasedAttributes> observableValue, StoreBasedAttributes storeBasedAttributes, StoreBasedAttributes t1) {
                MyTask loadProducts = new MyTask(()->{
                    System.out.println("trying to load products");
                    if(t1!=null){
                        if(!t1.getBarcodes().isEmpty()){
                            StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                            List<Product> products = storeBasedAttributesDAO.getProductsByBarcodes(t1.getBarcodes());
                            obsMatchingProducts.setAll(products);
                            return null;
                        }
                    }
                    obsMatchingProducts.clear();
                    return null;
                });
                loadProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        Throwable exception = workerStateEvent.getSource().getException();
                        if (exception != null) {
                            System.out.println("Error occurred: " + exception.getMessage());
                            // Optionally, log the full stack trace or handle the error further
                            exception.printStackTrace();
                        }
                    }
                });
                parentDelegate.listManager.addTaskToActiveList(
                        "loading matching Products",
                        "fetching the matching products from the database via barcodes",
                        loadProducts);
            }
        });
    }
    private void initTableMatchingProducts(){
        TableColumn<Product,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });


        tableMatchingProducts.getColumns().setAll(descriptionCol);
        tableMatchingProducts.setItems(obsMatchingProducts);
        tableMatchingProducts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableMatchingProducts.setPrefWidth(350);

        tableMatchingProducts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Product>() {
            @Override
            public void changed(ObservableValue<? extends Product> observableValue, Product product, Product t1) {
                System.out.println(" we hit something");
                if(t1!=null){
                    System.out.println("we are selecting a product :"+t1.getInvDescription());
                    if(t1.getId()==null){
                        System.out.println("the id is null");
                    } else {
                        System.out.println("the id is "+t1.getId());
                    }

                    MyTask loadingSbas = new MyTask(()->{
                        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
                        List<StoreBasedAttributes> sbas = storeBasedAttributesDAO.getStoreBasedAttributesByProduct(t1);
                        obsMatchingSbas.setAll(sbas);
                        System.out.println("we found "+sbas.size()+" matching sbas");
                        return null;
                    });

                    parentDelegate.listManager.addTaskToActiveList(
                            "loading sbas",
                            "loading all the sbas of the selected product",
                            loadingSbas
                    );
                } else {
                    obsMatchingSbas.clear();
                }
                setBtnVisibility();
            }
        });


    }
    private void initTableMatchingSbas(){
        TableColumn<StoreBasedAttributes,String> descCol = new TableColumn<>("description");
        descCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String s = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(s);
        });

        TableColumn<StoreBasedAttributes,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String m = cellData.getValue().getMasterCode();
            return new ReadOnlyStringWrapper(m);
        });

        TableColumn<StoreBasedAttributes,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            String h = cellData.getValue().getHope();
            return new ReadOnlyStringWrapper(h);
        });

        TableColumn<StoreBasedAttributes,String> familyCol = new TableColumn<>("family");
        familyCol.setCellValueFactory(cellData->{
            String f = cellData.getValue().getFamily();
            return new ReadOnlyStringWrapper(f);
        });

        TableColumn<StoreBasedAttributes,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDepartment();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> barcodesCol = new TableColumn<>("barcodes");
        barcodesCol.setCellValueFactory(cellData->{
            StringBuilder sb = new StringBuilder();

            cellData.getValue().getBarcodes().forEach(bar->{
                sb.append(bar).append("\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<StoreBasedAttributes,String> productCol = new TableColumn<>("product");
        productCol.setCellValueFactory(cellData->{
            String p = "";
            if(cellData.getValue().getProduct()==null){
                p="0";
            } else {
                p="1";
            }
            return new ReadOnlyStringWrapper(p);
        });
        tableMatchingSbas.getColumns().setAll(descCol,storeCol,masterCol,hopeCol,familyCol,departmentCol,barcodesCol,productCol);
        tableMatchingSbas.setItems(obsMatchingSbas);
    }
    private void initTableFilteredSbas(){
        TableColumn<StoreBasedAttributes,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String s = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(s);
        });

        TableColumn<StoreBasedAttributes,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDepartment();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            String h = cellData.getValue().getHope();
            return new ReadOnlyStringWrapper(h);
        });

        TableColumn<StoreBasedAttributes,String> barCol = new TableColumn<>("barcodes");
        barCol.setCellValueFactory(cellData->{
            StringBuilder sb = new StringBuilder();
            cellData.getValue().getBarcodes().forEach(bar->{
                sb.append(bar);
                sb.append("\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<StoreBasedAttributes,String> conBarsCol = new TableColumn<>("conflicting");
        conBarsCol.setCellValueFactory(cellData->{
            StringBuilder sb = new StringBuilder();
            cellData.getValue().getConflictingBarcodes().forEach(bar->{
                sb.append(bar);
                sb.append("\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<StoreBasedAttributes,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String m = cellData.getValue().getMasterCode();
            return new ReadOnlyStringWrapper(m);
        });

        tableFilteredSbas.getColumns().setAll(descriptionCol,storeCol,masterCol,departmentCol,hopeCol,barCol,conBarsCol);
        tableFilteredSbas.setItems(obsFilteredSbas);
    }
    private void refreshTableConflicts(){
        if(cbStores.getItems().size()<2){
            return;
        }
        List<StoreBasedAttributes> inConflict = new ArrayList<>();
        obsAllSbas.forEach(sba->{
            if(sba.getProduct()==null){
                if(cbStores.getValue().toLowerCase().compareTo("all")==0||sba.getStore().getName().compareTo(cbStores.getValue().trim())==0){
                    inConflict.add(sba);
                }
            }
        });
        obsSbaConflicts.setAll(inConflict);
    }
}

//String kouta = line.substring(53, 60);
//String fpaCode = line.substring(60, 61);
//String costPrice = line.substring(61,70);
//String salePrice = line.substring(70,78);
//String availability = line.substring(78, 79);

//String unit = line.substring(95,96);
//String status = line.substring(96,97);
