package com.example.pdfreader.Controllers.ByMenu.Suppliers;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.Controllers.States.SuppliersConfigState;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductCompleteDTO;
import com.example.pdfreader.DTOs.ProductWithSupplierCount;
import com.example.pdfreader.DTOs.SupplierWithProductCount;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.Supplier;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.Sinartiseis.InferingSuppliers;
import com.example.pdfreader.enums.ABInvoiceTypes;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SuppliersConfigView extends ChildController {
    private List<ProductCompleteDTO>  allProductsWithCountsList;
    private List<ProductCompleteDTO> departmentProductsWithCountList;
    public TableView<ProductCompleteDTO> productsTable = new TableView<>();
    private final ObservableList<ProductCompleteDTO> obsProductsTable = FXCollections.observableArrayList();
    public Text txtNumOfProducts;
    public Button btnTest;
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
    private  boolean listen = true;
    private final ListChangeListener<ProductCompleteDTO> updTxtOfCountingProducts = new ListChangeListener<ProductCompleteDTO>() {
        @Override
        public void onChanged(Change<? extends ProductCompleteDTO> change) {
            txtNumOfProducts.setText("products :"+productsTable.getItems().size());
        }
    };

    /**
     * we have this error
     *  we probably run into a conflict ΑGRINO GOURMET RISOTTOΤΡΟΥΦA - 200G ΑGRINO GOURMET RISOTTO ΤΡΟΥΦA 200G
     *  in different stores same product different hope => this results in duplicated product
     *  it is the only one product that has this problem
     *  currently we skip that barcode, but technically we have different multiple products created
     *
     *  Solution 1 : we don't add the hope barcodes into thw barcodes
     *  -> when we load a map for the scanner we use the hopes based on the store based attribute
     *
     *  1.create the new txt file with barcode to product
     *  2.load that product supplier relation accordingly
     *  3. save what is needed
     * @param hc
     */

    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        initProductsTable();
        initSuppsTable();
        initCBDeptsLogic();
        initButtons();

        fetchingTheData();

        btnTest.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                InferingSuppliers.printProductSupplierRelations(parentDelegate);
            }
        });
    }
    @Override
    public void addMyListeners() {
        obsProductsTable.addListener(updTxtOfCountingProducts);
    }
    @Override
    public void removeListeners(HelloController hc) {
        obsProductsTable.removeListener(updTxtOfCountingProducts);
    }
    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T) this;
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
            ProductCompleteDTO selectedProduct = productsTable.getSelectionModel().getSelectedItem();
            Supplier selectedSupplier = tableSupps.getSelectionModel().getSelectedItem().getSupplier();

            if (selectedProduct != null && selectedSupplier != null) {
                SupplierProductRelationDAO relationDao = new SupplierProductRelationDAO();
                if (!relationDao.relationExists(selectedProduct.getProduct(), selectedSupplier)) {
                    // Create and save the new relation
                    SupplierProductRelation newRelation = new SupplierProductRelation(selectedProduct.getProduct(), selectedSupplier);
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
            ProductCompleteDTO selectedProductWithCount = productsTable.getSelectionModel().getSelectedItem();
            SupplierWithProductCount selectedSupplierWithCount = tableSupps.getSelectionModel().getSelectedItem();

            if (selectedProductWithCount != null && selectedSupplierWithCount != null) {
                Product selectedProduct = selectedProductWithCount.getProduct();
                Supplier selectedSupplier = selectedSupplierWithCount.getSupplier();

                deleteSupplierProductRelation(selectedProduct, selectedSupplier);

                // Update tables
                updateProductInTable(selectedProductWithCount);
                updateSupplierInTable(selectedSupplier);
            } else {
                // Show error message if no product or supplier is selected
                showAlert("Selection Error", "Please select a product and a supplier.");
            }
        });
        btnLoadProms.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                SupplierProductRelationDAO supplierProductRelationDAO = new SupplierProductRelationDAO();
                SupplierDAO suppDao = new SupplierDAO();
                ProductDAO productDAO = new ProductDAO();
                int lines = 0;

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
                        lines++;
                        String[] parts = line.split(",", 2);
                        String supplierName = parts[0].trim();
                        String productName = parts[1].trim();

                        Product product = productMap.get(productName);
                        Supplier supplier = suppMap.get(supplierName);
                        System.out.println(supplierName+" "+productName);


                        if (supplier == null) {
                            supplier = new Supplier(supplierName);
                            newSuppliers.add(supplier);
                            System.out.println("adding supplier name "+supplierName);
                            suppMap.put(supplierName, supplier);
                        }
                        if (product != null) {

                            SupplierProductRelation spr =  new SupplierProductRelation(product, supplier);
                            if (!checkRelation(existingRelations,spr)){
                                newRelations.add(spr);
                            }
                        }
                    }
                    System.out.println("the lines are : "+lines);
                    System.out.println("the new suppliers are : "+newSuppliers.size());
                    System.out.println("the products are "+productMap.size());

                    // Perform database operations in a transactional scope
                    try {
                        supplierProductRelationDAO.saveAllRelations(newRelations, newSuppliers);
                        fetchingTheData();
                        //reloadSuppliersTable(suppDao); // reload supp table after loading supps from text
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
        btnSaveProm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                List<String> proms = tableSupps.getItems().stream().map(entry->entry.getSupplier().getName()).toList();
                //System.out.println(proms.size());


                SupplierProductRelationDAO sprDao = new SupplierProductRelationDAO();
                List<SupplierProductRelation> sprList = sprDao.findAll();

                Map<Supplier,Map<String,Product>> supplierProductMap = new HashMap<>();
                sprList.forEach(spr->{
                    supplierProductMap.computeIfAbsent(spr.getSupplier(), k -> new HashMap<>());
                    supplierProductMap.get(spr.getSupplier()).put(spr.getProduct().getInvmaster(),spr.getProduct());
                });


                List<String> list1 = new ArrayList<>();
                List<String> list2 = new ArrayList<>();
                supplierProductMap.keySet().forEach(supplier -> {
                    supplierProductMap.get(supplier).keySet().forEach(product -> {
                        list1.add(supplier.getName());
                        list2.add(product);
                    });
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
        btnCalculate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MyTask calculateTask = new MyTask(()->{
                    InferingSuppliers.forAllDocuments();
                    return null;
                });
                calculateTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        fetchingTheData();
                    }
                });

                parentDelegate.listManager.addTaskToActiveList(
                        "calculate task",
                        "calculate task",
                        calculateTask
                );
            }
        });
    }
    private void initCBDeptsLogic() {
        cbDepts.setItems(obsDeptOptions);
        cbFamily.setItems(obsFamilyOptions);
        cbDepts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                System.out.println("- - - - on cbDept change - - - - - - :::"+t1);
                if(t1!=null){
                    if (t1.compareTo("all") == 0) {
                        //if(!allProductsWithCountsList.isEmpty())
                        obsProductsTable.setAll(allProductsWithCountsList);
                        obsFamilyOptions.clear();
                    } else {
                        List<ProductCompleteDTO> selection = new ArrayList<>();
                        for (ProductCompleteDTO pwc : allProductsWithCountsList) {
                            if (!pwc.getStoreBasedAttributesMap().isEmpty()) {
                                pwc.getStoreBasedAttributesMap().values().forEach(sba->{
                                    if(sba.getDepartment().compareTo(t1)==0){
                                        selection.add(pwc);
                                    }
                                });
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

                List<ProductCompleteDTO> selection = new ArrayList<>();
                //System.out.println("t1 not null");
                if (t1.compareTo("all") == 0) {
                    //System.out.println("t1 all");
                    obsProductsTable.setAll(departmentProductsWithCountList);
                } else {
                    for (ProductCompleteDTO pwc : departmentProductsWithCountList) {
                        if (!pwc.getStoreBasedAttributesMap().isEmpty()) {
                            if (pwc.getStoreBasedAttributesMap().values().stream().toList().get(0).getFamily().compareTo(t1) == 0) {
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
    private void loadDepartmentChoices(ObservableList<ProductCompleteDTO> products){
        //obsDeptOptions.clear();
        ArrayList<String> tempChoices = new ArrayList<>();
        tempChoices.add("all");
        for(ProductCompleteDTO pwc:products){
            for(StoreBasedAttributes sba : pwc.getStoreBasedAttributesMap().values()){
                if(!tempChoices.contains(sba.getDepartment())){
                    tempChoices.add(sba.getDepartment());
                }
            }
        }
        Platform.runLater(()->{
            obsDeptOptions.clear();
            obsDeptOptions.setAll(tempChoices);
            if (!obsDeptOptions.isEmpty()) {
                cbDepts.getSelectionModel().select(0);
            }
        });
    }
    public void loadFamilyChoices(List<ProductCompleteDTO> products){
        ArrayList<String> tempFamilyChoices = new ArrayList<>();
        System.out.println("- - - - loading family choices - - - - - - ");
        for(ProductCompleteDTO pwc:products){
            for(StoreBasedAttributes sba : pwc.getStoreBasedAttributesMap().values()){
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
        TableColumn<ProductCompleteDTO,String> descCol = new TableColumn<>("description");
        descCol.setCellValueFactory(cellData->{
            String value = cellData.getValue().getProduct().getInvDescription();
            return new ReadOnlyStringWrapper(value);
        });

        TableColumn<ProductCompleteDTO,Number> suppsCol = new TableColumn<>("suppliers");
        suppsCol.setCellValueFactory(cellData->{
            Number n = cellData.getValue().getSupplierNames().size();
            return new ReadOnlyObjectWrapper<>(n);
        });

        suppsCol.setCellFactory(column -> new TableCell<ProductCompleteDTO, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.toString());
                    Long productId = getTableView().getItems().get(getIndex()).getProduct().getId();

                    Tooltip tooltip = new Tooltip();
                    tooltip.setText("Loading...");
                    setTooltip(tooltip);

                    // Fetch supplier names in a background thread to avoid UI freezing
                    Task<List<String>> fetchSuppliersTask = new Task<>() {
                        @Override
                        protected List<String> call() {
                            ProductDAO productDao = new ProductDAO();
                            return productDao.getSupplierNamesForProduct(productId);
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

        TableColumn<ProductCompleteDTO,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            if (cellData.getValue().getStoreBasedAttributesMap().isEmpty()){
                return new ReadOnlyStringWrapper("no hop yet");
            }
            String value = cellData.getValue().getStoreBasedAttributesMap().values().stream().toList().get(0).getHope();
            return new ReadOnlyStringWrapper(value);
        });

        TableColumn<ProductCompleteDTO,String> famCol = new TableColumn<>("family");
        famCol.setCellValueFactory(cellData->{
            List<String> fams = new ArrayList<>();
            //System.out.println(cellData.getValue().getProduct().getStoreBasedAttributes().get(0).getFamily());

            cellData.getValue().getStoreBasedAttributesMap().values().forEach(sba->{
                if(!fams.contains(sba.getFamily())){
                    fams.add(sba.getFamily());
                }
            });
            StringBuilder sb = new StringBuilder();
            fams.forEach(fam->{
                sb.append(fam+"\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
        });

        TableColumn<ProductCompleteDTO,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{

            List<String> values = new ArrayList<>();
            cellData.getValue().getStoreBasedAttributesMap().values().forEach(sba->{
                if(!values.contains(sba.getDepartment())){
                    values.add(sba.getDepartment());
                }


            });
            StringBuilder sb = new StringBuilder();
            values.forEach(val->{
                sb.append(val+"\n");
            });
            return new ReadOnlyStringWrapper(sb.toString());
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
        List<SupplierWithProductCount> suppliersWithCounts = supplierDao.getSuppliersWithProductCount();
        List<SupplierWithProductCount> supplierWithCountsList = suppliersWithCounts.stream()
                .map(objects -> new SupplierWithProductCount(objects.getSupplier(), objects.getProductCount()))
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
    private void updateProductInTable(ProductCompleteDTO product) {
        //ProductDAO productDao = new ProductDAO();
        //int updatedCount = productDao.getSupplierCountForProduct(product.getProduct().getId());
        //ProductWithSupplierCount updatedProductWithCount = new ProductWithSupplierCount(product.getProduct(), (long) updatedCount);
        System.out.println("entered the product refreshing");
        ProductCompleteDTO pc = ProductCompleteDTO.fetchProductDetailsByProduct(HibernateUtil.getEntityManagerFactory().createEntityManager(), product.getProduct());
        System.out.println("got the pc, its supps size is "+pc.getSupplierNames().size());




        for (ProductCompleteDTO pwc : allProductsWithCountsList) {
            if (pwc.getProduct().getId().equals(product.getProduct().getId())) {
                Platform.runLater(()->{
                    System.out.println("we found it on the list and we try to replace.  current size is "+pwc.getSupplierNames().size());
                    allProductsWithCountsList.set(allProductsWithCountsList.indexOf(pwc),pc);
                    if(obsProductsTable.contains(pwc)){
                        obsProductsTable.set(obsProductsTable.indexOf(pwc),pc);
                        productsTable.getSelectionModel().select(pc);
                    }
                    System.out.println("new size is "+allProductsWithCountsList.get(allProductsWithCountsList.indexOf(pc)).getSupplierNames().size());
                });
                break;
            }
        }


        // Find and update the product in the table
        Platform.runLater(()->{
            productsTable.refresh();
        });

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


        Platform.runLater(()->{
            tableSupps.refresh();
        });

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
                if(e.getProduct().getInvmaster().compareTo(spr.getProduct().getInvmaster())==0){
                    return true;
                }
            }
        }
        return false;
    }
    private void fetchingTheData(){
        MyTask myTask = new MyTask(()->null);
        myTask.setTaskLogic(()->{
            List<SupplierWithProductCount> resultList = new ArrayList<>();
            List<ProductCompleteDTO> results = new ArrayList<>();

            CompletableFuture<Void> fetchingProducts = CompletableFuture.runAsync(()->{
                List<ProductCompleteDTO> allCompleteDtos = ProductCompleteDTO.fetchAllProductDetails(HibernateUtil.getEntityManagerFactory().createEntityManager());
                allProductsWithCountsList= allCompleteDtos;
                results.addAll(allCompleteDtos);
                System.out.println("loaded products with supplier count task "+results.size());

                Platform.runLater(()->{
                    myTask.setMyDescription(myTask.getMyDescription()+"\n fetching products ended");
                });
            });
            CompletableFuture<Void> fetchingSuppliers = CompletableFuture.runAsync(()->{

                SupplierDAO supplierDao = new SupplierDAO();
                List<SupplierWithProductCount> suppliersWithCounts = supplierDao.getSuppliersWithProductCount();

                List<SupplierWithProductCount> supplierWithCountsList = suppliersWithCounts.stream()
                        .map(objects -> new SupplierWithProductCount(objects.getSupplier(), objects.getProductCount()))
                        .toList();

                resultList.addAll(supplierWithCountsList);

                Platform.runLater(()->{
                    myTask.setMyDescription(myTask.getMyDescription()+"\n fetching suppliers ended");
                });
            });
            CompletableFuture<Void> afterFetched = CompletableFuture.allOf(fetchingProducts,fetchingSuppliers);

            afterFetched.thenRun(()->{
                loadDepartmentChoices(FXCollections.observableArrayList(results));
                Platform.runLater(()->{
                    //from first completable
                    obsProductsTable.setAll(results);
                    productsTable.refresh();

                    //from second completable
                    obsSuppliers.setAll(resultList);
                    if(parentDelegate.suppliersConfigState == null){
                        cbDepts.getSelectionModel().select("all");
                    }
                    myTask.setMyDescription(myTask.getMyDescription()+"\nloading completed");
                });
            });

            return null;
        });
        myTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                workerStateEvent.getSource().getException().printStackTrace();
                System.out.println(workerStateEvent.getSource().getMessage());
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "fetching the data",
                "loading the data",
                myTask
        );

    }
}
