package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductWithAttributes;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.Sinartiseis.ImportItemAndPosFiles;
import com.example.pdfreader.enums.StoreNames;
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
    private ListChangeListener<Product>  btnVisibilityListener = change -> setBtnVisibility();
    private ListChangeListener<StoreBasedAttributes> allSbasListener = change -> {
        obsAllSbasTable.setAll(obsAllSbas);
        refreshTableConflicts();
    };
    private ListChangeListener<Product> allProductsListener = change -> {
        Platform.runLater(()->{
            obsProductsTable.setAll(obsAllProducts);
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
        initTables();
        initButtons();
        loadContent();
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

    /*

     */



    /**
    keep this, we may need this when we change the rest of the program
     */
    private void loadProductWithAttributes(){
        List<ProductWithAttributes> products = ProductWithAttributes.getAllProductsWithAttributes(HibernateUtil.getEntityManagerFactory().createEntityManager());
        products.forEach(product->{
            System.out.println("the product "+product.getProduct().getInvDescription()+" has "+product.getAttributes().size()+" attributes");
        });
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
                    ImportItemAndPosFiles r = new ImportItemAndPosFiles(parentDelegate);
                    r.initiateLoadingProcess();
                    r=null;
                    return null;
                });
                readingFiles.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        System.out.println("an error here\n"+workerStateEvent.getSource().getMessage());
                        workerStateEvent.getSource().getException().printStackTrace();
                    }
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
