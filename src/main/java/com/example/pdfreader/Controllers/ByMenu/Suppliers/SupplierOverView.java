package com.example.pdfreader.Controllers.ByMenu.Suppliers;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.HibernateUtil;
import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.DAOs.SupplierDAO;
import com.example.pdfreader.DAOs.SupplierProductRelationDAO;
import com.example.pdfreader.DTOs.ProductDetailsDTO;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.Supplier;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class SupplierOverView extends ChildController {

    public TableView<ProductDetailsDTO> tableSupplierProducts;
    public Button btnUnlink;
    public Button btnLink;
    public ComboBox<Supplier> cbSuppliers;
    public ComboBox<String> cbDepartment;
    public TableView<ProductDetailsDTO> tableFreeProducts;
    public TextField txtFilter;
    private List<ProductDetailsDTO> allProducts = new ArrayList<>();
    private ObservableList<ProductDetailsDTO> obsSuppProducts = FXCollections.observableArrayList();
    private ObservableList<ProductDetailsDTO> obsFreeProducts = FXCollections.observableArrayList();

    private final EventHandler<ActionEvent> handleTxtFilter = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent actionEvent) {
            if(!txtFilter.getText().isEmpty()){
                String term = txtFilter.getText().toLowerCase().trim();
                List<ProductDetailsDTO> filtered = new ArrayList<>();
                allProducts.forEach(product->{
                    if(product.getInvDescription().toLowerCase().contains(term)){
                        filtered.add(product);
                    }
                });
                obsFreeProducts.setAll(filtered);
            } else {
                obsFreeProducts.setAll(filterFreeProducts());
            }
        }
    };




    @Override
    public void initialize(HelloController hc) {
        System.out.println("we are on the correct controller");
        this.parentDelegate = hc;




        initSuppTable();
        initFreeProductsTable();

        initCBDepartments();
        initCBSuppliers();

        initBtns();

        MyTask loadingSupplierTask = getSuppliersTask();

        MyTask loadingProductsTask = getProductsTask();
        loadingProductsTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                List<String> deptOptions = new ArrayList<>();
                deptOptions.add("ALL");
                allProducts.forEach(product -> {
                    if(!product.getStoreBasedAttributes().isEmpty()){
                        product.getStoreBasedAttributes().forEach(sba->{
                            if(sba.getDepartment().trim().isEmpty()){
                                System.out.println("an empty department");
                                System.out.println(sba.getDescription());
                                if(sba.getProduct()!=null){
                                    System.out.println(sba.getProduct().getInvDescription());
                                } else {
                                    System.out.println("null product on the sba");
                                }

                            }
                            if(sba.getDepartment()!=null){
                                if(!deptOptions.contains(sba.getDepartment())){
                                    deptOptions.add(sba.getDepartment());
                                }
                            }
                        });
                    }
                });
                deptOptions.add("null");
                cbDepartment.setItems(FXCollections.observableList(deptOptions));
                cbDepartment.getSelectionModel().select(0);
                if(!cbSuppliers.getItems().isEmpty()){
                    cbSuppliers.getSelectionModel().select(0);
                }
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading suppliers",
                "loading suppliers",
                loadingSupplierTask
        );
        parentDelegate.listManager.addTaskToActiveList(
                "loading products",
                "fetching all products from the Database",
                loadingProductsTask
        );

    }

    @Override
    public void addMyListeners() {
        txtFilter.addEventHandler(ActionEvent.ACTION,handleTxtFilter);

    }

    @Override
    public void removeListeners(HelloController hc) {
        txtFilter.removeEventHandler(ActionEvent.ACTION,handleTxtFilter);

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

    private void initSuppTable(){
        TableColumn<ProductDetailsDTO,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });

        tableSupplierProducts.getColumns().setAll(descriptionCol);
        tableSupplierProducts.setItems(obsSuppProducts);
    }
    private void initFreeProductsTable(){
        TableColumn<ProductDetailsDTO,String> descriptionCol = new TableColumn<>();
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<ProductDetailsDTO,String> suppliersCol = new TableColumn<>();
        suppliersCol.setCellValueFactory(cellData->{
            String c =String.valueOf(cellData.getValue().getSupplierNames().size());
            return new ReadOnlyStringWrapper(c);
        });

        tableFreeProducts.getColumns().setAll(descriptionCol,suppliersCol);
        tableFreeProducts.setItems(obsFreeProducts);
    }
    private void initCBDepartments(){
        cbDepartment.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                txtFilter.removeEventHandler(ActionEvent.ACTION,handleTxtFilter);
                txtFilter.setText("");
                txtFilter.addEventHandler(ActionEvent.ACTION,handleTxtFilter);
                obsFreeProducts.setAll(filterFreeProducts());
            }
        });
    }
    private void initCBSuppliers(){
        cbSuppliers.setCellFactory(callback->new ListCell<Supplier>(){
            @Override
            protected void updateItem(Supplier item,boolean empty){
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        cbSuppliers.setButtonCell(new ListCell<Supplier>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        cbSuppliers.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Supplier>() {
            @Override
            public void changed(ObservableValue<? extends Supplier> observableValue, Supplier s, Supplier t1) {
                System.out.println(" filtering happened for suppliers");
                if(t1!=null){
                    List<ProductDetailsDTO> filtered = new ArrayList<>();
                    allProducts.forEach(product->{
                        product.getSupplierNames().forEach(supplier->{
                            if(t1.getName().compareTo(supplier)==0){
                                filtered.add(product);
                            }
                        });
                    });
                    Platform.runLater(()->{
                        obsSuppProducts.setAll(filtered);
                    });
                }
            }
        });

    }
    private List<ProductDetailsDTO> filterFreeProducts(){
        System.out.println(" filtering products");
        List<ProductDetailsDTO> filtered = new ArrayList<>();
        allProducts.forEach(product->{
            if(product.getSupplierNames().isEmpty()){
                if(cbDepartment.getSelectionModel().getSelectedItem()!=null)
                if(cbDepartment.getSelectionModel().getSelectedItem().toLowerCase().compareTo("all")==0){
                    filtered.add(product);
                } else if (cbDepartment.getSelectionModel().getSelectedItem().compareTo("null")==0){
                    if(product.getStoreBasedAttributes().isEmpty()){
                        filtered.add(product);
                    }
                } else {
                    if(!product.getStoreBasedAttributes().isEmpty()){
                        product.getStoreBasedAttributes().forEach(sba->{
                            if(cbDepartment.getSelectionModel().getSelectedItem().compareTo(sba.getDepartment())==0){
                                if(!filtered.contains(product)){
                                    filtered.add(product);
                                }

                            }
                        });
                    }
                }
            }
        });
        return filtered;
    }
    private MyTask getSuppliersTask(){
        MyTask loadSuppliers = new MyTask(()->{
            SupplierDAO supplierDAO = new SupplierDAO();
            ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
            suppliers.addAll(supplierDAO.findAll());
            Platform.runLater(()->{
                cbSuppliers.setItems(suppliers);
            });
            return null;
        });

        return loadSuppliers;
    }
    private MyTask getProductsTask(){
        MyTask loadProducts = new MyTask(()->{
            allProducts.clear();
            HelpingFunctions.setStartTime();
            allProducts.addAll(ProductDetailsDTO.fetchAllProductDetailsWithSuppliers(HibernateUtil.getEntityManagerFactory().createEntityManager()));
            HelpingFunctions.setEndAndPrint("fetching dtos");
            return null;
        });

        loadProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println("error at loadProducts \n"+workerStateEvent.getSource().getMessage());
                workerStateEvent.getSource().getException().printStackTrace();
            }
        });
        /*
         * initiating the departments
         */
        return loadProducts;

    }
    private void initBtns(){
        initBtnLink();
        initBtnUnlink();
    }
    private void initBtnLink(){
        btnLink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ProductDetailsDTO productDTO = tableFreeProducts.getSelectionModel().getSelectedItem();
                Supplier supplier = cbSuppliers.getSelectionModel().getSelectedItem();
                if(supplier==null){
                    return;
                }
                if(productDTO==null){
                    return;
                }
                SupplierProductRelationDAO relationDao = new SupplierProductRelationDAO();
                ProductDAO productDAO = new ProductDAO();
                Product product = productDAO.getProduct(productDTO.getId());
                SupplierProductRelation newRelation = new SupplierProductRelation(product, supplier);
                relationDao.save(newRelation);

                reloadTables();
            }
        });
    }
    private void initBtnUnlink(){
        btnUnlink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ProductDetailsDTO productDTO = tableSupplierProducts.getSelectionModel().getSelectedItem();
                Supplier supplier = cbSuppliers.getSelectionModel().getSelectedItem();

                if(productDTO==null){
                    return;
                }
                if(supplier==null){
                    return;
                }
                ProductDAO productDAO = new ProductDAO();
                Product product = productDAO.getProduct(productDTO.getId());

                SupplierProductRelationDAO relationDao = new SupplierProductRelationDAO();
                List<SupplierProductRelation> relationsToDelete = relationDao.findRelationsByProductAndSupplier(product, supplier);

                for (SupplierProductRelation relation : relationsToDelete) {
                    relationDao.delete(relation);
                }
                reloadTables();
            }
        });
    }
    private void reloadTables(){
        System.out.println("reload tables");
        MyTask loadingProducts = getProductsTask();
        MyTask loadingSuppliers = getSuppliersTask();
        int supplierIndex = cbSuppliers.getSelectionModel().getSelectedIndex();
        loadingSuppliers.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                parentDelegate.listManager.addTaskToActiveList(
                        "reloading after unling",
                        "reloading pridcuts",
                        loadingProducts
                );
            }
        });
        loadingProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Platform.runLater(()->{
                    obsFreeProducts.setAll(filterFreeProducts());
                    cbSuppliers.getSelectionModel().select(supplierIndex);
                });

            }
        });
        parentDelegate.listManager.addTaskToActiveList(
                "reloading after unlink",
                "reloading suppliers",
                loadingSuppliers
        );
        //txtFilter.setText("");
    }




}
