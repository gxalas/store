package com.example.pdfreader.Controllers;

import com.example.pdfreader.Controllers.States.ProductViewState;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductDTO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.PosEntry;
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
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductsView extends ChildController{
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
            MyTask myTask = new MyTask(()->{
                if(t1!=null){
                    DocEntryDAO docEntryDao = new DocEntryDAO();
                    PosEntryDAO posEntryDao = new PosEntryDAO();
                    activeProductDocEntries = docEntryDao.getDocEntriesByProductMasterCode(t1.getMaster());
                    activeProductPosEntries = posEntryDao.getPosEntriesByProductMasterCode(t1.getMaster());

                    List<DocEntry> docResults = filterDocEntries();
                    List<PosEntry> posResults = filterPosEntries();

                    Platform.runLater(()->{
                        obsDocEntries.setAll(docResults);
                        obsPosEntries.setAll(posResults);
                    });
                }
                return null;
            });


            parentDelegate.listManager.addTaskToActiveList(
                    "loading Docs and Pos Entries - BREAK THEM - OR REMOVE THEM",
                    "Fetching for "+t1.getDescriptions(),
                    myTask
            );
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

    }
    @Override
    public void removeListeners(HelloController hc) {

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
                    System.out.println(parentDelegate.productViewState.productDto().getDescriptions()+"is supposed to be selected");
                    tableProducts.getFocusModel().focus(0);
                    System.out.println("the selected product is "+tableProducts.getSelectionModel().getSelectedItem().getDescriptions());
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
            //long start = System.nanoTime();
            productDtoList = productDAO.getAllProductsWithDocumentCountAndDescriptions();
            List<ProductDTO> filtered = filterProducts();
            //productDtoList = filterProducts();
            //long end = System.nanoTime();
            //System.out.println("the time is : "+((end-start)/1_000_000.00)+" mills");
            //ObservableList<ProductDTO> data = FXCollections.observableArrayList(productDtoList);
            Platform.runLater(()->{
                obsProducts.setAll(filtered);
            });
            return null;
        });
        return loadProductDtos;
    }
    private void initProductsTable(){

        TableColumn<ProductDTO,String> productIdCol = new TableColumn<>("id");
        productIdCol.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<ProductDTO,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellValue ->{
            if (cellValue.getValue().getDescriptions().isEmpty()){
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(cellValue.getValue().getDescriptions().get(0));
        });

        TableColumn<ProductDTO, Number> descriptionsSizeCol = new TableColumn<>("Descriptions Size");
        descriptionsSizeCol.setCellValueFactory(cellData -> {
            ProductDTO product = cellData.getValue();
            int size = product.getDescriptions() != null ? product.getDescriptions().size() : 0;
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
            String newValue = cellData.getValue().getHopeCodeByStrore(StoreNames.getStoreByName(cbStores.getValue()));
            return new ReadOnlyObjectWrapper<>(newValue);
        });



        tableProducts.getColumns().setAll(productIdCol,descriptionCol,descriptionsSizeCol,
                masterCol,docCounterCol,numHopesCol,hopeCol);
        tableProducts.setItems(obsProducts);

        tableProducts.getSelectionModel().selectedItemProperty().addListener(productsChangeListener);
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
            return new ReadOnlyStringWrapper(cellData.getValue().storeName.getDescription());
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
                if(!product.getDescriptions().isEmpty()){
                    if(product.getDescriptions().get(0).toLowerCase().contains(searchBar.getText().toLowerCase())){
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
}
