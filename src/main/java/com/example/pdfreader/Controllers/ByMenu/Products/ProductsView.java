package com.example.pdfreader.Controllers.ByMenu.Products;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.Controllers.States.ProductViewState;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductDTO;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ProductsView extends ChildController {
    private final ObservableList<ProductDTO> obsProducts = FXCollections.observableArrayList();
    private List<ProductDTO> productDtoList = new ArrayList<>();
    private final ObservableList<DocEntry> obsDocEntries = FXCollections.observableArrayList();
    private final ObservableList<PosEntry> obsPosEntries = FXCollections.observableArrayList();
    private List<DocEntry> activeProductDocEntries = new ArrayList<>();
    private List<PosEntry> activeProductPosEntries = new ArrayList<>();
    public TableView<ProductDTO> tableProducts;
    public Button testBtn;
    public Text txtProductsNum;
    public TableView<DocEntry> tableDocEntries;
    public TableView<PosEntry> tablePosEntries;
    public ChoiceBox<String> cbStores;
    public TextField searchBar;
    private final ChangeListener<ProductDTO> productsChangeListener = new ChangeListener<ProductDTO>() {
        @Override
        public void changed(ObservableValue<? extends ProductDTO> observableValue, ProductDTO productDTO, ProductDTO t1) {
            if(t1!=null){
                MyTask myTask = new MyTask(()->{
                    DocEntryDAO docEntryDao = new DocEntryDAO();
                    PosEntryDAO posEntryDao = new PosEntryDAO();
                    activeProductDocEntries = docEntryDao.getDocEntriesByProductMasterCode(t1.getMaster());
                    activeProductPosEntries = posEntryDao.getPosEntriesByProductMasterCode(t1.getMaster());
                    System.out.println("active pos entries is "+activeProductPosEntries.size());

                    List<DocEntry> docResults = filterDocEntries();
                    List<PosEntry> posResults = filterPosEntries();

                    Platform.runLater(()->{
                        obsDocEntries.setAll(docResults);
                        obsPosEntries.setAll(posResults);
                    });
                    return null;
                });

                myTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        System.out.println("the fetching failed\n"+workerStateEvent.getSource().getMessage());
                        workerStateEvent.getSource().getException().printStackTrace();
                    }
                });


                parentDelegate.listManager.addTaskToActiveList(
                        "loading Docs and Pos Entries - BREAK THEM - OR REMOVE THEM",
                        "Fetching for "+getBasicDescription(t1.getStoreBasedAttributes()),
                        myTask
                );
            }

            //parentDelegate.listManager.getActiveTasksList().add(0,myTask);
            //parentDelegate.listManager.getActiveTasksList().add(0,myTask);

        }
    };
    private final EventHandler<ActionEvent> searchBarListener = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent actionEvent) {
            System.out.println("event triggered");
            MyTask task = new MyTask(()->{
                List<ProductDTO> results = filterProducts();
                Platform.runLater(()->{
                    obsProducts.setAll(results);
                });
                return null;
            });
            //task.setMyTitle("search bar search");
            //task.setMyDescription("filtering using the search bar");

            parentDelegate.listManager.addTaskToActiveList(
                    "search bar",
                    "filtering results based on search bar text",
                    task
            );

            //parentDelegate.listManager.getActiveTasksList().add(0,task);
        }
    };
    private final ListChangeListener<ProductDTO> productCounterListener = new ListChangeListener<ProductDTO>() {
        @Override
        public void onChanged(Change<? extends ProductDTO> change) {
            txtProductsNum.setText("there are "+ obsProducts.size()+" products");
        }
    };
    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        MyTask loadingProducts = loadingProductDtosTask();
        loadingProducts.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println(workerStateEvent.getSource().getMessage());
                workerStateEvent.getSource().getException().printStackTrace();
            }
        });
        parentDelegate.listManager.addTaskToActiveList(
                "loading Products",
                "fetching producrs with DTO (Document count and Descriptions)",
                loadingProducts
        );
        //parentDelegate.listManager.getActiveTasksList().add(0,loadingProductDtosTask());
        searchBar.setOnAction(searchBarListener);
        obsProducts.addListener(productCounterListener);
        initCBStores();
        initProductsTable();
        initDocEntries();
        initPosEntries();
        initTestBtn();
    }
    private void initCBStores() {
        cbStores.getItems().setAll(StoreNames.stringValues());
        cbStores.getItems().remove(StoreNames.NONE.getName());

        cbStores.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (StoreNames.getStoreByName(t1)!=null){
                System.out.println("changed to "+StoreNames.getStoreByName(t1).getDescription());
                obsDocEntries.setAll(filterDocEntries());
                obsPosEntries.setAll(filterPosEntries());
                tableProducts.refresh();
                //tablePosEntries.getItems().setAll(filterPosEntries());
            } else {
                System.err.println("got null on store retrieving");
            }
        });
        cbStores.getSelectionModel().select(0);
    }
    @Override
    public void addMyListeners() {
        tableProducts.getSelectionModel().selectedItemProperty().addListener(productsChangeListener);

    }
    @Override
    public void removeListeners(HelloController hc) {
        tableProducts.getSelectionModel().selectedItemProperty().removeListener(productsChangeListener);

    }
    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T)this;
    }

    @Override
    public void setState() {
        String inputTxt = searchBar.getText();
        StoreNames store = StoreNames.getStoreByName(cbStores.getValue());
        ProductDTO product = tableProducts.getSelectionModel().getSelectedItem();
        parentDelegate.productViewState = new ProductViewState(inputTxt,store,product);
    }

    @Override
    public void getPreviousState() {
        if(parentDelegate.productViewState!=null){
            System.out.println("getting previous state");
            searchBar.setText(parentDelegate.productViewState.searchTxt());
            cbStores.getSelectionModel().select(parentDelegate.productViewState.selectedStore().getName());
            searchBar.fireEvent(new ActionEvent());
            if (parentDelegate.productViewState.productDto()!=null){
                Platform.runLater(()->{
                    tableProducts.requestFocus();
                    //tableProducts.getSelectionModel().clearAndSelect();
                    tableProducts.getSelectionModel().select(parentDelegate.productViewState.productDto());
                    System.out.println(parentDelegate.productViewState.productDto().getDescription()+"is supposed to be selected");
                    tableProducts.getFocusModel().focus(0);
                    System.out.println("the selected product is "+tableProducts.getSelectionModel().getSelectedItem().getDescription());
                });

            }
            System.out.println("the size of the products is "+obsProducts.size());

        } else {
            System.out.println("no previous state for product view");
        }

    }


    private MyTask loadingProductDtosTask(){
        MyTask loadProductDtos = new MyTask(()->{
            ProductDAO productDAO = new ProductDAO();
            //productDtoList = productDAO.getAllProductsWithDocumentCountAndDescriptions();
            productDtoList = productDAO.createProductDTOs();
            List<ProductDTO> filtered = filterProducts();
            Platform.runLater(()->{
                obsProducts.setAll(filtered);
            });
            return null;
        });
        return loadProductDtos;
    }
    private void initProductsTable(){
        TableColumn<ProductDTO,String> productIdCol = new TableColumn<>("document code");
        productIdCol.setCellValueFactory(cellData->{
            String c = cellData.getValue().getCode();
            return new ReadOnlyStringWrapper(c);
        });


        TableColumn<ProductDTO,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellValue ->{
            if (cellValue.getValue().getDescription().isEmpty()){
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(cellValue.getValue().getDescription());
        });

        TableColumn<ProductDTO, Number> numOfSbas = new TableColumn<>("sba's attached");
        numOfSbas.setCellValueFactory(cellData -> {
            ProductDTO product = cellData.getValue();
            int size = cellData.getValue().getStoreBasedAttributes().size();
            //int size = product.getDescriptions() != null ? product.getDescriptions().size() : 0;
            return new ReadOnlyIntegerWrapper(size);
        });

        TableColumn<ProductDTO,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(new PropertyValueFactory<>("master"));

        TableColumn<ProductDTO,Long> docCounterCol = new TableColumn<>("documents");
        docCounterCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDocumentCount()));

        TableColumn<ProductDTO,Number> numHopesCol = new TableColumn<>("num of hopes");
        numHopesCol.setCellValueFactory(cellData->{
            Number value = cellData.getValue().getStoreBasedAttributes().size();
            return new ReadOnlyObjectWrapper<>(value);
        });

        TableColumn<ProductDTO,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            AtomicReference<String> h = new AtomicReference<>("");
            cellData.getValue().getStoreBasedAttributes().forEach(sba->{
                if(sba.getStore().getName().compareTo(cbStores.getValue())==0){
                    h.set(sba.getHope());
                }
            });
            if(h.get().compareTo("")==0&& !cellData.getValue().getStoreBasedAttributes().isEmpty()){
                cellData.getValue().getStoreBasedAttributes().forEach(sba->{
                    if(!sba.getHope().isEmpty()&&h.get().compareTo("")==0){
                        h.set("("+cellData.getValue().getStoreBasedAttributes().get(0).getHope()+")");
                    }
                });
            }
            return new ReadOnlyObjectWrapper<>(h.get());
        });
        tableProducts.getColumns().setAll(productIdCol,descriptionCol,
                masterCol,docCounterCol,hopeCol,numOfSbas);
        tableProducts.setItems(obsProducts);
    }

    private void initDocEntries(){
        TableColumn<DocEntry,String> docIdCol = new TableColumn<>("document id");
        docIdCol.setCellValueFactory(cellData->{
            String result = cellData.getValue().getDocument().getDocumentId();
            return new ReadOnlyStringWrapper(result);
        });

        TableColumn<DocEntry, Date> docDateCol = new TableColumn<>("Date");
        docDateCol.setCellValueFactory(cellData->{
            Date date = cellData.getValue().getDate();
            return new ReadOnlyObjectWrapper<>(date);
        });

        docDateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(ABUsualInvoice.format.format(item));
                }
            }
        });

        TableColumn<DocEntry, BigDecimal> totalValueCol = new TableColumn<>("total price");
        totalValueCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        TableColumn<DocEntry, BigDecimal> unitPriceCol = new TableColumn<>("unit Price");
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<DocEntry,BigDecimal> unitsCol = new TableColumn<>("Units");
        unitsCol.setCellValueFactory(new PropertyValueFactory<>("units"));

        tableDocEntries.getColumns().setAll(docIdCol,docDateCol,unitPriceCol,unitsCol,totalValueCol);
        tableDocEntries.setItems(obsDocEntries);
    }
    private void initPosEntries() {
        TableColumn<PosEntry,String> posDescCol = new TableColumn<>("description");
        posDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<PosEntry,Date> dateCol = new TableColumn<>("date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(ABUsualInvoice.format.format(item));
                }
            }
        });

        TableColumn<PosEntry,BigDecimal> quantityCol = new TableColumn<>("quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PosEntry,BigDecimal> moneyCol = new TableColumn<>("income");
        moneyCol.setCellValueFactory(new PropertyValueFactory<>("money"));

        TableColumn<PosEntry,BigDecimal> priceCol = new TableColumn<>("price");
        priceCol.setCellValueFactory(cellData ->{
            return new ReadOnlyObjectWrapper<>(cellData.getValue().getPrice());
        });

        TableColumn<PosEntry,String> storeCol = new TableColumn<>("Store");
        storeCol.setCellValueFactory(cellData->{
            return new ReadOnlyStringWrapper(cellData.getValue().storeName.getName());
        });

        tablePosEntries.getColumns().setAll(dateCol,storeCol,posDescCol,quantityCol,moneyCol,priceCol);
        tablePosEntries.setItems(obsPosEntries);
    }


    private void initTestBtn(){
        testBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Platform.runLater(()->{
                    System.out.println("this is a test button currently doing nothing");
                    //listManager.productMap.put("123123",new Product(listManager,"123123","the description","1210001212"));
                    //products.add(new Product(parentDelegate.listManager, "testCode","test desc","test ean"));
                });
            }
        });
    }

    private ArrayList<ProductDTO> filterProducts(){
        System.out.println("filtering triggered");
        ArrayList<ProductDTO> filtered = new ArrayList<>();
        if(!searchBar.getText().trim().isEmpty()){
            for(ProductDTO product : productDtoList) {
                if(!product.getDescription().isEmpty()){
                    if(product.getDescription().toLowerCase().contains(searchBar.getText().toLowerCase())){
                        filtered.add(product);
                    }
                }
            }
        } else {
            return new ArrayList<>(productDtoList);
        }
        return filtered;
    }

    private List<DocEntry> filterDocEntries() {
        String store = cbStores.getValue();
        List<DocEntry> toReturn = new ArrayList<>();
        for(DocEntry entry : activeProductDocEntries){
            if(entry.getDocument()!=null){
                if (entry.getDocument().getStore().getName().compareTo(store)==0){
                    toReturn.add(entry);
                }
            } else {
                System.out.println("document is null on this docEntry ");
            }
        }

        return toReturn;
    }
    private List<PosEntry> filterPosEntries(){
        String store = cbStores.getValue();
        List<PosEntry> toReturn = new ArrayList<>();
        for(PosEntry entry : activeProductPosEntries){
            if (entry.getStoreName().getName().compareTo(store)==0){
                toReturn.add(entry);
            }
        }
        return toReturn;
    }
    private String getBasicDescription(List<StoreBasedAttributes> sbas){

        AtomicReference<String> h = new AtomicReference<>("");
        for (StoreBasedAttributes sba : sbas) {
            if (!sba.getDescription().isEmpty()) {
                h.set(sba.getDescription());
                return h.get();
            }
        }
        return h.get();
    }
}
