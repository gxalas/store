package com.example.pdfreader.Controllers;

import com.example.pdfreader.Controllers.States.SuppliersConfigState;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductWithSupplierCount;
import com.example.pdfreader.DTOs.SupplierWithProductCount;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Entities.Supplier;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.MyTaskState;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.enums.ABInvoiceTypes;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SuppliersConfigView extends ChildController{
    public TableView<ProductWithSupplierCount> productsTable = new TableView<>();
    private final ObservableList<ProductWithSupplierCount> obsProductsTable = FXCollections.observableArrayList();
    public Text txtNumOfProducts;
    private List<ProductWithSupplierCount>  allProductsWithCountsList;
    private List<ProductWithSupplierCount> departmentProductsWithCountList;
    public Button btnAddSupp;
    public Button btnLink;
    public Button btnUnlink;
    public Button btnSaveProm;
    public Button btnLoadProms;
    public Button btnCalculate;
    public TextField txtSuppName;
    public TableView<SupplierWithProductCount> tableSupps;
    private final ObservableList<SupplierWithProductCount> obsSuppliers = FXCollections.observableArrayList();
    public ComboBox<String> cbDepts;
    public ComboBox<String> cbFamily;
    private final ObservableList<String> obsDeptOptions = FXCollections.observableArrayList();
    private final ObservableList<String> obsFamilyOptions = FXCollections.observableArrayList();
    //private MyTask loadProductsTask = loadProductsWithSuppliersCountTask();
    //private final MyTask loadSuppliersTask = loadSuppliersWithProductCountTask();
    private  boolean listen = true;
    private final IntegerProperty finishedTasks = new SimpleIntegerProperty(); // we are waiting for both task to finish before
    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        finishedTasks.set(0);
        parentDelegate.listManager.addTaskToActiveList(
                "loading the products",
                "loading the products here",
                loadProductsWithSuppliersCountTask());
        //parentDelegate.listManager.getActiveTasksList().add(0, loadProductsTask);
        parentDelegate.listManager.addTaskToActiveList(
                "loading suppliers",
                "loading the suppliers",
                loadSuppliersWithProductCountTask()
        );
        //parentDelegate.listManager.getActiveTasksList().add(0, loadSuppliersTask);

        finishedTasks.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (t1.intValue()==2&&parentDelegate.suppliersConfigState==null){
                    Platform.runLater(()->{
                        System.out.println("- - - - - - initing combo buttons - - - - - -  ");
                        cbDepts.getSelectionModel().select("all");
                    });
                }
            }
        });

        obsProductsTable.addListener(new ListChangeListener<ProductWithSupplierCount>() {
            @Override
            public void onChanged(Change<? extends ProductWithSupplierCount> change) {
                txtNumOfProducts.setText("products :"+productsTable.getItems().size());
            }
        });

        initProductsTable();
        initSuppsTable();
        initCBDepts();
        initButtons();

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
        String dept = cbDepts.getValue();
        String family = cbFamily.getValue();
        parentDelegate.suppliersConfigState = new SuppliersConfigState(dept,family);
    }
    @Override
    public void getPreviousState() {
        if(parentDelegate.suppliersConfigState == null){
            return;
        }
        System.out.println("- - - - - - - trying to load previous state - - - - - - - - - ");

        obsDeptOptions.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                if (parentDelegate.suppliersConfigState!=null){
                    System.out.println("previous state exists");
                    System.out.println(obsDeptOptions.size()+" the size of the department choices");
                    //cbDepts.getItems().forEach(System.out::println);
                    if (cbDepts.getItems().contains(parentDelegate.suppliersConfigState.department())){
                        Platform.runLater(()->{
                            System.out.println(" - - - - - - should select "+parentDelegate.suppliersConfigState.department()+" department - - - - - ");
                            cbDepts.getSelectionModel().select(parentDelegate.suppliersConfigState.department());
                            System.out.println("- - - - - - - - the selected is "+cbDepts.getSelectionModel().getSelectedItem());
                        });
                    }

                } else {
                    System.out.println("previous state does not exist");
                }
                obsDeptOptions.removeListener(this);
            }
        });

        obsFamilyOptions.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                System.out.println("- - - - - on success - - - - - ");
                if (parentDelegate.suppliersConfigState.family()!=null){
                    System.out.println("family exists");
                    if(obsFamilyOptions.contains(parentDelegate.suppliersConfigState.family())){
                        System.out.println("family contained");
                        Platform.runLater(()->{
                            System.out.println("setting  from the state + + + + + + + + + + + + = + ");
                            cbFamily.getSelectionModel().select(parentDelegate.suppliersConfigState.family());
                        });

                    } else {
                        System.out.println("previous value didn't exist");
                    }
                } else {
                    System.out.println(" the family was null");
                }
                obsFamilyOptions.removeListener(this);
            }
        });






    }
    private void initButtons() {
        btnAddSupp.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String suppName = txtSuppName.getText();
                System.out.println("button pressed for name "+suppName);
                if (!suppName.isEmpty() && suppName.length()<20){
                    System.out.println("attempting to save the supp");
                    SupplierDAO supplierDAO = new SupplierDAO();
                    Supplier supplier = new Supplier(suppName);
                    supplierDAO.save(supplier);
                    reloadSuppliersTable(supplierDAO); //reload after adding supp
                }
            }
        });

        btnLink.setOnAction(event -> {
            Product selectedProduct = productsTable.getSelectionModel().getSelectedItem().getProduct();
            Supplier selectedSupplier = tableSupps.getSelectionModel().getSelectedItem().getSupplier();

            if (selectedProduct != null && selectedSupplier != null) {

                SupplierProductRelationDAO relationDao = new SupplierProductRelationDAO();
                if (!relationDao.relationExists(selectedProduct, selectedSupplier)) {
                    // Create and save the new relation
                    SupplierProductRelation newRelation = new SupplierProductRelation(selectedProduct, selectedSupplier);
                    relationDao.save(newRelation);

                    // Update the product's supplier count in the table
                    updateProductInTable(selectedProduct);
                    updateSupplierInTable(selectedSupplier);
                } else {
                    // Show message that the relation already exists
                    showAlert("Relation already exists", "This product-supplier relation already exists.");
                }
            } else {
                showAlert("Selection Error", "You have to select both a Product and a Relation.");
                // Show error message - no product or supplier selected
            }
        });

        btnUnlink.setOnAction(event->{
            ProductWithSupplierCount selectedProductWithCount = productsTable.getSelectionModel().getSelectedItem();
            SupplierWithProductCount selectedSupplierWithCount = tableSupps.getSelectionModel().getSelectedItem();

            if (selectedProductWithCount != null && selectedSupplierWithCount != null) {
                Product selectedProduct = selectedProductWithCount.getProduct();
                Supplier selectedSupplier = selectedSupplierWithCount.getSupplier();

                deleteSupplierProductRelation(selectedProduct, selectedSupplier);

                // Update tables
                updateProductInTable(selectedProduct);
                updateSupplierInTable(selectedSupplier);
            } else {
                // Show error message if no product or supplier is selected
                showAlert("Selection Error", "Please select a product and a supplier.");
            }
        });
        btnSaveProm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                List<String> proms = tableSupps.getItems().stream().map(entry->entry.getSupplier().getName()).toList();
                //System.out.println(proms.size());


                SupplierProductRelationDAO sprDao = new SupplierProductRelationDAO();
                List<SupplierProductRelation> sprList = sprDao.findAll();
                List<String> list1 = new ArrayList<>();
                List<String> list2 = new ArrayList<>();
                sprList.forEach(value->{
                    list1.add(value.getSupplier().getName());
                    list2.add(value.getProduct().getMaster());
                    //System.out.println(+" + "+);
                });
                System.out.println(sprList.size());

                // File path

                String filePath = "appFiles/Proms/output.txt"; ; // Replace with your file path
                File file = new File(filePath);
                // Create parent directories if they do not exist
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try {
                    // Check if the file does not exist and create a new file
                    if (file.createNewFile()) {
                        System.out.println("File created: " + file.getName());
                    } else {
                        System.out.println("File already exists.");
                    }
                } catch (IOException e) {
                    System.out.println("An error occurred while creating the file.");
                    e.printStackTrace();
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    for (int i = 0; i < list1.size(); i++) {
                        // Write a pair to the file
                        writer.write(list1.get(i) + ", " + list2.get(i));
                        // New line
                        writer.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btnLoadProms.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                SupplierProductRelationDAO supplierProductRelationDAO = new SupplierProductRelationDAO();
                SupplierDAO suppDao = new SupplierDAO();
                ProductDAO productDAO = new ProductDAO();

                // Use try-with-resources to ensure BufferedReader is closed after use
                try (BufferedReader reader = new BufferedReader(new FileReader("appFiles/Proms/output.txt"))) {
                    // Create maps for suppliers and products for quick lookup
                    Map<String, Supplier> suppMap = suppDao.findAll().stream()
                            .collect(Collectors.toMap(Supplier::getName, Function.identity()));
                    Map<String, Product> productMap = productDAO.getAllProductsAsMap();

                    List<SupplierProductRelation> newRelations = new ArrayList<>();
                    List<Supplier> newSuppliers = new ArrayList<>();
                    List<SupplierProductRelation> existingRelations = supplierProductRelationDAO.findAll();


                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",", 2);
                        String supplierName = parts[0].trim();
                        String productName = parts[1].trim();

                        Product product = productMap.get(productName);
                        Supplier supplier = suppMap.get(supplierName);

                        if (product != null) {
                            if (supplier == null) {
                                supplier = new Supplier(supplierName);
                                newSuppliers.add(supplier);
                                suppMap.put(supplierName, supplier);
                            }
                            SupplierProductRelation spr =  new SupplierProductRelation(product, supplier);
                            if (!checkRelation(existingRelations,spr)){
                                newRelations.add(spr);
                            }
                        }
                    }

                    // Perform database operations in a transactional scope
                    try {
                        supplierProductRelationDAO.saveAllRelations(newRelations, newSuppliers);
                        reloadSuppliersTable(suppDao); // reload supp table after loading supps from text

                    } catch (Exception e) {
                        // Log the exception for debugging purposes
                        e.printStackTrace();
                        // Handle the exception, e.g., show an error message to the user
                    }

                    System.out.println("- - - - - finished - - - - - ");
                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle file reading exceptions, e.g., show an error message
                }
            }
        });
        btnCalculate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MyTask calculateTask = new MyTask(()->{
                    assignSuppliersAndUpdateRelations();
                    return null;
                });
                MyTask loadProducts = loadProductsWithSuppliersCountTask();
                MyTask loadSuppliers = loadSuppliersWithProductCountTask();

                loadProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        if(!obsDeptOptions.isEmpty()){
                            Platform.runLater(()->{
                                cbDepts.getSelectionModel().select(0);
                            });
                        }
                    }
                });



                calculateTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        parentDelegate.listManager.addTaskToActiveList(
                                "loading products",
                                "loading the product results - again",
                                loadProducts
                        );
                        parentDelegate.listManager.addTaskToActiveList(
                                "loading suppliers",
                                "loading suppliers",
                                loadSuppliers
                        );
                    }
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "calculate task",
                        "calculate task",
                        calculateTask
                );

                //MyTask duplicated = loadProductsTask;
               // if(loadProductsTask.getMyState().compareTo(MyTaskState.RUNNING)!=0){



               // }
            }
        });


    }
    private void initCBDepts() {
        cbDepts.setItems(obsDeptOptions);
        cbFamily.setItems(obsFamilyOptions);

        cbDepts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                System.out.println("- - - - on cbDept change - - - - - - :::"+t1);
                if(t1!=null){
                    if (t1.compareTo("all") == 0) {
                        obsProductsTable.setAll(allProductsWithCountsList);
                        obsFamilyOptions.clear();
                    } else {
                        List<ProductWithSupplierCount> selection = new ArrayList<>();
                        for (ProductWithSupplierCount pwc : allProductsWithCountsList) {
                            if (!pwc.getProduct().getStoreBasedAttributes().isEmpty()) {
                                if (pwc.getProduct().getStoreBasedAttributes().get(0).getDepartment().compareTo(t1) == 0) {
                                    selection.add(pwc);
                                }
                            }
                        }
                        departmentProductsWithCountList = selection;
                        obsProductsTable.setAll(departmentProductsWithCountList);
                        loadFamilyChoices(selection);
                        Platform.runLater(()->{
                            listen = true;
                            System.out.println("setting from the department the listener");
                            cbFamily.getSelectionModel().select("all");
                        });

                    }
                }
            }
        });


        cbFamily.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (!listen){
                    return;
                }

                if (t1 == null) {
                    System.out.println("t1 is null");
                    // Skip processing for intermediate null state
                    return;
                }
                System.out.println("- - - - - - - CBFamily triggered - - - - - - - :::::"+t1);

                List<ProductWithSupplierCount> selection = new ArrayList<>();
                //System.out.println("t1 not null");
                if (t1.compareTo("all") == 0) {
                    //System.out.println("t1 all");
                    obsProductsTable.setAll(departmentProductsWithCountList);
                } else {
                    //System.out.println("t1 not all");

                    for (ProductWithSupplierCount pwc : departmentProductsWithCountList) {
                        if (!pwc.getProduct().getStoreBasedAttributes().isEmpty()) {
                            if (pwc.getProduct().getStoreBasedAttributes().get(0).getFamily().compareTo(t1) == 0) {
                                selection.add(pwc);
                            }
                        }
                    }
                    System.out.println("selection size "+selection.size());
                    obsProductsTable.setAll(selection);
                }
            }
        });

    }
    private void loadDepartmentChoices(ObservableList<ProductWithSupplierCount>products){
        obsDeptOptions.clear();
        ArrayList<String> tempChoices = new ArrayList<>();
        tempChoices.add("all");
        for(ProductWithSupplierCount pwc:products){
            for(StoreBasedAttributes sba : pwc.getProduct().getStoreBasedAttributes()){
                if(!tempChoices.contains(sba.getDepartment())){
                    tempChoices.add(sba.getDepartment());
                }
            }
        }
        obsDeptOptions.setAll(tempChoices);
    }
    public void loadFamilyChoices(List<ProductWithSupplierCount> products){
        ArrayList<String> tempFamilyChoices = new ArrayList<>();
        System.out.println("- - - - loading family choices - - - - - - ");
        for(ProductWithSupplierCount pwc:products){
            for(StoreBasedAttributes sba : pwc.getProduct().getStoreBasedAttributes()){
                if(!tempFamilyChoices.contains(sba.getFamily())){
                    tempFamilyChoices.add(sba.getFamily());
                }
            }
        }
        tempFamilyChoices.add(0,"all");
        Platform.runLater(()->{
            System.out.println("setting a different family options");
            listen = false;
            obsFamilyOptions.setAll(tempFamilyChoices);
            listen = true;
        });



    }
    private void initProductsTable() {
        TableColumn<ProductWithSupplierCount,String> descCol = new TableColumn<>("description");
        descCol.setCellValueFactory(cellData->{
            String value = cellData.getValue().getProduct().getDescription();
            return new ReadOnlyStringWrapper(value);
        });

        TableColumn<ProductWithSupplierCount,Number> suppsCol = new TableColumn<>("suppliers");
        suppsCol.setCellValueFactory(cellData->{
            Number n = cellData.getValue().getSupplierCount();
            return new ReadOnlyObjectWrapper<>(n);
        });


        suppsCol.setCellFactory(column -> new TableCell<ProductWithSupplierCount, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.toString());
                    Product product = getTableView().getItems().get(getIndex()).getProduct();

                    Tooltip tooltip = new Tooltip();
                    tooltip.setText("Loading...");
                    setTooltip(tooltip);

                    // Fetch supplier names in a background thread to avoid UI freezing
                    Task<List<String>> fetchSuppliersTask = new Task<>() {
                        @Override
                        protected List<String> call() {
                            ProductDAO productDao = new ProductDAO();
                            return productDao.getSupplierNamesForProduct(product.getId());
                        }
                    };
                    fetchSuppliersTask.setOnSucceeded(event -> {
                        List<String> supplierNames = fetchSuppliersTask.getValue();
                        tooltip.setText(String.join("\n", supplierNames));
                    });
                    new Thread(fetchSuppliersTask).start();
                }
            }
        });

        TableColumn<ProductWithSupplierCount,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            if (cellData.getValue().getProduct().getStoreBasedAttributes().isEmpty()){
                return new ReadOnlyStringWrapper("-1");
            }
            String value = cellData.getValue().getProduct().getStoreBasedAttributes().get(0).getHope();
            return new ReadOnlyStringWrapper(value);
        });

        TableColumn<ProductWithSupplierCount,String> famCol = new TableColumn<>("family");
        famCol.setCellValueFactory(cellData->{
            //System.out.println(cellData.getValue().getProduct().getStoreBasedAttributes().get(0).getFamily());
            StringBuilder sb = new StringBuilder();
            for(StoreBasedAttributes sba:cellData.getValue().getProduct().getStoreBasedAttributes()){
                if(!sb.isEmpty()){
                    sb.append("\n");
                }
                sb.append(sba.getFamily());
            }
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<ProductWithSupplierCount,Number> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            Number value = -1;
            if(!cellData.getValue().getProduct().getStoreBasedAttributes().isEmpty()){
                value = Integer.parseInt(cellData.getValue().getProduct().getStoreBasedAttributes().get(0).getDepartment().trim());
            }
            return new ReadOnlyObjectWrapper<>(value);
        });

        productsTable.getColumns().setAll(descCol,suppsCol,hopeCol,famCol,departmentCol);
        productsTable.setItems(obsProductsTable);
    }
    private void initSuppsTable() {
        TableColumn<SupplierWithProductCount,String> nameCol = new TableColumn<>("name");
        nameCol.setCellValueFactory(cellData ->{
            String name = cellData.getValue().getSupplier().getName();
            return new ReadOnlyStringWrapper(name);
        });

        TableColumn<SupplierWithProductCount,Number> idCol = new TableColumn<>("id");
        idCol.setCellValueFactory(cellData->{
            long id = cellData.getValue().getSupplier().getId();
            return new ReadOnlyObjectWrapper<>(id);
        });

        TableColumn<SupplierWithProductCount, Void> editColumn = new TableColumn<>("Actions");

        Callback<TableColumn<SupplierWithProductCount, Void>, TableCell<SupplierWithProductCount, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<SupplierWithProductCount, Void> call(final TableColumn<SupplierWithProductCount, Void> param) {
                final TableCell<SupplierWithProductCount, Void> cell = new TableCell<>() {

                    private final Button btn = new Button("Edit");

                    {
                        btn.setOnAction((event) -> {
                            Supplier supplier = getTableView().getItems().get(getIndex()).getSupplier();
                            showEditDialog(supplier);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        editColumn.setCellFactory(cellFactory);
        TableColumn<SupplierWithProductCount,Number> productCountCol = new TableColumn<>("products");
        productCountCol.setCellValueFactory(cellData->{
            Number count = cellData.getValue().getProductCount();
            return new ReadOnlyObjectWrapper<>(count);
        });

        tableSupps.getColumns().setAll(idCol,nameCol,editColumn,productCountCol);
        tableSupps.setItems(obsSuppliers);
    }
    private void reloadSuppliersTable(SupplierDAO supplierDao){
        List<Object[]> suppliersWithCounts = supplierDao.getSuppliersWithProductCount();
        List<SupplierWithProductCount> supplierWithCountsList = suppliersWithCounts.stream()
                .map(objects -> new SupplierWithProductCount((Supplier) objects[0], ((Long) objects[1]).intValue()))
                .collect(Collectors.toList());

        obsSuppliers.setAll(supplierWithCountsList);
        //loadProductsWithSuppliersCountThread().start();
        //loadProductsTable();
    }
    private void showEditDialog(Supplier supplier) {
        // Create the custom dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit Supplier");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OTHER); // Add Delete button
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, deleteButtonType);

        // Create the supplier name label and field
        VBox vbox = new VBox();
        vbox.setSpacing(10);

        TextField nameField = new TextField();
        nameField.setText(supplier.getName());
        vbox.getChildren().add(new Label("Supplier Name:"));
        vbox.getChildren().add(nameField);

        // Set content
        dialog.getDialogPane().setContent(vbox);

        //save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    // Update supplier name
                    String newName = nameField.getText();
                    supplier.setName(newName);

                    // Update supplier in the database
                    SupplierDAO supplierDao = new SupplierDAO();
                    try {
                        supplierDao.update(supplier);
                        dialog.close(); // Close the dialog
                        reloadSuppliersTable(supplierDao); // reload suppliers after editing supps
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Optionally, show an error message to the user
                    }
                }
        );
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    if (confirmDeletion()) {
                        SupplierDAO supplierDao = new SupplierDAO();
                        try {
                            supplierDao.delete(supplier.getId());
                            dialog.close(); // Close the dialog
                            reloadSuppliersTable(supplierDao); // Reload suppliers after deleting
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Optionally, show an error message to the user
                        }
                    }
                }
        );

        dialog.showAndWait();
    }
    private boolean confirmDeletion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Supplier");
        alert.setContentText("Are you sure you want to delete this supplier?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void updateProductInTable(Product product) {
        ProductDAO productDao = new ProductDAO();
        int updatedCount = productDao.getSupplierCountForProduct(product.getId());
        ProductWithSupplierCount updatedProductWithCount = new ProductWithSupplierCount(product, (long) updatedCount);

        // Find and update the product in the table
        for (ProductWithSupplierCount pwc : productsTable.getItems()) {
            if (pwc.getProduct().getId().equals(product.getId())) {
                pwc.setSupplierCount(updatedCount);
                break;
            }
        }
        productsTable.refresh();
    }
    private void updateSupplierInTable(Supplier supplier) {
        SupplierDAO supplierDAO = new SupplierDAO();
        int updatedCount = supplierDAO.getProductCountForSupplier(supplier);
        SupplierWithProductCount supplierWithProductCount = new SupplierWithProductCount(supplier, updatedCount);

        // Find and update the product in the table
        for (SupplierWithProductCount swp : tableSupps.getItems()) {
            if (swp.getSupplier().getId().equals(supplier.getId())) {
                swp.setProductCount(updatedCount);
                break;
            }
        }
        tableSupps.refresh();
    }
    private void deleteSupplierProductRelation(Product product, Supplier supplier) {
        SupplierProductRelationDAO relationDao = new SupplierProductRelationDAO();
        List<SupplierProductRelation> relationsToDelete = relationDao.findRelationsByProductAndSupplier(product, supplier);

        for (SupplierProductRelation relation : relationsToDelete) {
            relationDao.delete(relation);
        }
    }
    private boolean checkRelation(List<SupplierProductRelation> existing, SupplierProductRelation spr){
        for(SupplierProductRelation e : existing){
            if (e.getSupplier().getName().compareTo(spr.getSupplier().getName())==0){
                if(e.getProduct().getMaster().compareTo(spr.getProduct().getMaster())==0){
                    return true;
                }
            }
        }
        return false;
    }
    public void assignSuppliersAndUpdateRelations() {
        SupplierProductRelationDAO relationDAO = new SupplierProductRelationDAO();
        DocumentDAO documentDAO = new DocumentDAO(new DBErrorDAO(new ErrorEventManager()));

        // Load all relations and documents once at the beginning
        List<SupplierProductRelation> allRelations = relationDAO.findAll();
        List<Document> allDocuments = documentDAO.getAllDocuments();
        System.out.println("there were "+allDocuments.size());
        allDocuments = allDocuments.stream().filter(doc->doc.getType().compareTo(ABInvoiceTypes.TIMOLOGIO)==0).collect(Collectors.toList());
        System.out.println("there are "+allDocuments.size());
        System.out.println("the relations are "+allRelations.size());

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

                    // Check and create new relations
                    for (Product product : document.getProducts()) {
                        if (!productSupplierMap.containsKey(product) || !productSupplierMap.get(product).contains(supplier)) {
                            SupplierProductRelation newRelation = new SupplierProductRelation(product, supplier);
                            allRelations.add(newRelation);
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
        documentDAO.updateDocuments(allDocuments);
       // documentDAO.saveAll(allDocuments);
        relationDAO.saveAll(allRelations);
    }
    private MyTask loadSuppliersWithProductCountTask(){

        MyTask loadSuppliersWithProductCountTask = new MyTask(()->{
            List<SupplierWithProductCount> resultList = new ArrayList<>();
            SupplierDAO supplierDao = new SupplierDAO();
            List<Object[]> suppliersWithCounts = supplierDao.getSuppliersWithProductCount();

            List<SupplierWithProductCount> supplierWithCountsList = suppliersWithCounts.stream()
                    .map(objects -> new SupplierWithProductCount((Supplier) objects[0], ((Long) objects[1]).intValue()))
                    .toList();

            resultList.addAll(supplierWithCountsList);
            Platform.runLater(()->{
                obsSuppliers.setAll(resultList);
            });
            return null;
        });
        loadSuppliersWithProductCountTask.setMyTitle("Fetching Suppliers");
        loadSuppliersWithProductCountTask.setMyDescription("get all the Suppliers with Product Count");

        loadSuppliersWithProductCountTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                // Additional onSucceeded logic
                MyTask myTask = (MyTask) event.getSource();
                System.out.println("a demo method to show how we can access the task attributes from this methods "+myTask.getMyTitle());
                finishedTasks.set(finishedTasks.get()+1);

            }
        });
        loadSuppliersWithProductCountTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                finishedTasks.set(finishedTasks.get()+1);
                Platform.runLater(()->{
                    System.out.println(" ERROR AT LOADING SUPPLIERS WITH PRODUCT COUNT");
                });
            }
        });

        return loadSuppliersWithProductCountTask;
    }
    private MyTask loadProductsWithSuppliersCountTask(){

        MyTask loadProductWithSupplierCountTask = new MyTask(()->{
            List<ProductWithSupplierCount> results = new ArrayList<>();
            ProductDAO productDao = new ProductDAO();
            List<Object[]> productsWithCounts = productDao.getProductsWithSupplierCount();

            allProductsWithCountsList = productsWithCounts.stream()
                    .map(objects -> new ProductWithSupplierCount((Product) objects[0], (Long) objects[1]))
                    .toList();

            results.addAll(allProductsWithCountsList);


            Platform.runLater(()->{
                loadDepartmentChoices(FXCollections.observableArrayList(results));
                System.out.println("loaded products with supplier count task "+results.size());
                obsProductsTable.setAll(results);
            });

            //System.out.println("the supp config  page is initing\n the products are "+ allProductsWithCountsList.size() );
            return null;
        });
        loadProductWithSupplierCountTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println("nothing");
                finishedTasks.set(finishedTasks.get()+1);
            }
        });
        loadProductWithSupplierCountTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                finishedTasks.set(finishedTasks.get()+1);
                System.out.println(" ERROR AT LOADING THE PRODUCTS WITH SUPPLIER COUNT");
            }
        });
        loadProductWithSupplierCountTask.setMyTitle("Load Products");
        loadProductWithSupplierCountTask.setMyDescription("Loading Products with Supplier Count");

        return loadProductWithSupplierCountTask;
    }
}
