package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.DAOs.SupplierProductRelationDAO;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SupplierOverView extends ChildController {
    public ComboBox<String> cbSuppliers;
    public TableView<Product> tableSupplierProducts;
    public Button btnUnlink;
    public Button btnLink;
    public ComboBox<String> cbDepartment;
    public TableView<Product> tableFreeProducts;
    private List<Product> allProducts = new ArrayList<>();
    private ObservableList<Product> obsSuppProducts = FXCollections.observableArrayList();

    private List<SupplierProductRelation> allRelations = new ArrayList<>();

    @Override
    public void initialize(HelloController hc) {

        this.parentDelegate = hc;

        MyTask loadProducts = new MyTask(()->{
            ProductDAO productDAO = new ProductDAO();
            allProducts.clear();
            allProducts.addAll(productDAO.getAllProducts());
            return null;
        });

        loadProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                AtomicInteger counter = new AtomicInteger();
                List<String> deptOptions = new ArrayList<>();
                deptOptions.add("ALL");
                allProducts.forEach(product -> {
                    String option = "null";
                    if(!product.getStoreBasedAttributes().isEmpty()){
                        if(product.getStoreBasedAttributes().get(0).getDepartment()!=null){
                            option = product.getStoreBasedAttributes().get(0).getDepartment();
                        }
                    }
                    if(option.compareTo("null")==0){
                        counter.getAndIncrement();
                        System.out.println("product with null department \n"+product.getInvDescription()+" "+product.getInvmaster());
                        System.out.println(" sbas "+product.getStoreBasedAttributes().isEmpty());
                    }

                    if(!deptOptions.contains(option)){
                        deptOptions.add(option);
                    }
                });
                System.out.println("products with null depts "+counter.get()+", all products "+allProducts.size());
                cbDepartment.setItems(FXCollections.observableList(deptOptions));
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading products",
                "fetching all products from the Database",
                loadProducts
        );



        MyTask loadRelations = new MyTask(()->{
            SupplierProductRelationDAO supplierProductRelationDAO = new SupplierProductRelationDAO();
            allRelations.clear();
            allRelations.addAll(supplierProductRelationDAO.findAll());
            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading relation",
                "fetching all Supplier - Product Relations fro the database",
                loadRelations
        );






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

    private void initSuppTable(){
        TableColumn<Product,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getInvDescription();
            return new ReadOnlyStringWrapper(d);
        });

        tableSupplierProducts.getColumns().setAll(descriptionCol);
        tableSupplierProducts.setItems(obsSuppProducts);
    }
}
