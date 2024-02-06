package com.example.pdfreader.Controllers;

import com.example.pdfreader.Controllers.States.FilterInvoicesState;
import com.example.pdfreader.Controllers.States.PreviewFileViewState;
import com.example.pdfreader.DAOs.DBErrorDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.SupplierDAO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Supplier;
import com.example.pdfreader.HelloApplication;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.StoreSummary;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedListener;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class FilterInvoicesView extends ChildController{
    private List<Document> dbDocuments= new ArrayList<>();
    private final ObservableList<Document> obsFilteredDocs = FXCollections.observableArrayList(new ArrayList<>());
    private final ObservableList<DocEntry> obsFilteredEntries = FXCollections.observableArrayList(new ArrayList<>());
    private final BidiMap<StoreNames, Integer> storeMap = new DualHashBidiMap<>();
    private final Map<String, StoreSummary> summaryMap = new HashMap<>();
    @FXML
    public TableView<StoreNames> filterSumTable = new TableView<>();
    @FXML
    public TableView<Document> qDocsTable = new TableView<>();
    @FXML
    public TableView<DocEntry> qEntriesTable = new TableView<>();
    @FXML
    public ComboBox<String> cbSuppliers;
    private ObservableList<String> obsSuppliersOptions = FXCollections.observableArrayList();
    public ComboBox<PromTypes> cbProm;
    public ComboBox<ABInvoiceTypes> cbType;
    public DatePicker qMinDate;
    public DatePicker qMaxDate;
    @FXML
    public Button btnGoToPreview = new Button();
    @FXML
    public Text txtSum;

    private final DocumentsImportedListener importedListener = new DocumentsImportedListener() {
        @Override
        public void documentsImported(DocumentsImportedEvent evt) {
            loadDates();
            obsFilteredDocs.setAll(filteredDocuments());
            System.out.println("the size of filtered docs is: "+obsFilteredDocs.size());
        }
    };
    private final ListChangeListener<String> storeListListener  =new ListChangeListener<String>() {
        @Override
        public void onChanged(Change<? extends String> change) {
            obsFilteredDocs.setAll(filteredDocuments());
            System.out.println("changed");
        }
    };
    @FXML
    public  ListView<String> storeList = new ListView<>();


    @Override
    public void initialize(HelloController hc) {
        parentDelegate = hc;
        initStoreList();
        initSummaryMap();
        initComboBoxes();
        initSummaryTable();
        initDocQueryTable();
        initQEntriesTable();
        initBtnGotoPreview();
        initDatePickers();

        MyTask task = getLoadDataTask();


        parentDelegate.listManager.addTaskToActiveList(
                "load documents",
                "getting Documents using DAO and errorDB",
                task
        );


        //parentDelegate.listManager.getActiveTasksList().add(0, task);

        if (parentDelegate.filterInvoicesState == null){
            System.out.println("no state for filtering invoices");
        }
    }

    private void initStoreList() {
        storeList.setItems(FXCollections.observableList(StoreNames.stringValues()));
        storeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    @Override
    public void addMyListeners() {
        parentDelegate.addDocumentProcessedListener(importedListener);
        parentDelegate.actFilterInvoices.setDisable(true);
        //probably the storelist listener is removed because the controller gets removed
        storeList.getSelectionModel().getSelectedItems().addListener(storeListListener);

    }

    @Override
    public void removeListeners(HelloController hc) {
        hc.removeDocumentProcessedListener(importedListener);
        hc.actFilterInvoices.setDisable(false);
        storeList.getSelectionModel().getSelectedItems().removeListener(storeListListener);
    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T) this;
    }
    @Override
    public void setState() {
        ObservableList<String> stores =  storeList.getSelectionModel().getSelectedItems();
        //int store = cbStores.getSelectionModel().getSelectedIndex();
        int type = cbType.getSelectionModel().getSelectedIndex();
        int prom = cbProm.getSelectionModel().getSelectedIndex();
        Date start =null;
        Date end =null;
        if (qMinDate.valueProperty().get()!=null){
            start = Date.from(qMinDate.valueProperty().get().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (qMaxDate.valueProperty().get()!=null){
            end = Date.from(qMaxDate.valueProperty().get().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        parentDelegate.filterInvoicesState = new FilterInvoicesState(stores,type,prom,start,end);
    }
    @Override
    public void getPreviousState() {
        if (parentDelegate.filterInvoicesState!=null){
            for(String s:parentDelegate.filterInvoicesState.stores()){
                storeList.getSelectionModel().select(s);
            }
            //cbStores.getSelectionModel().select(parentDelegate.filterInvoicesState.store());
            cbType.getSelectionModel().select(parentDelegate.filterInvoicesState.type());
            cbProm.getSelectionModel().select(parentDelegate.filterInvoicesState.prom());
            if (parentDelegate.filterInvoicesState.start()!=null){
                qMinDate.valueProperty().setValue(parentDelegate.filterInvoicesState.start().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate());
            }
            if (parentDelegate.filterInvoicesState.end()!=null){
                qMaxDate.valueProperty().setValue(parentDelegate.filterInvoicesState.end().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate());
            }

        } else {
            System.out.println("- - - - - - - - - - - - - - - -no previous state was saved");
        }

    }

    private MyTask getLoadDataTask() {
        MyTask loadDataTask = new MyTask(()->{
            System.out.println("data loading started ...");
            DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
            DocumentDAO documentDAO = new DocumentDAO(dbErrorDAO);

            dbDocuments =  documentDAO.getAllDocuments();
            Platform.runLater(()->{
                obsFilteredDocs.setAll(filteredDocuments());
                //System.out.println(" data loaded");
            });

            return null;
        });

        return loadDataTask;
    }
    private void initSummaryMap() {
        for(StoreNames store : StoreNames.values()){
            storeMap.put(store,storeMap.size());
            summaryMap.put(store.getName(),new StoreSummary(store.getName(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        }
    }
    private void initDatePickers() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR,2021);
        cal.set(Calendar.MONTH,0);
        cal.set(Calendar.DATE,0);
        qMinDate.valueProperty().setValue(LocalDate.from(cal.getTime().toInstant().atZone(ZoneId.systemDefault())));
        cal.set(Calendar.YEAR,2024);
        cal.set(Calendar.MONTH,11);
        cal.set(Calendar.DATE,31);
        qMaxDate.valueProperty().setValue(LocalDate.from(cal.getTime().toInstant().atZone(ZoneId.systemDefault())));
    }
    public void initBtnGotoPreview(){
        btnGoToPreview.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(qDocsTable.getSelectionModel().getSelectedItem()!=null){
                    parentDelegate.loadController("preview-file-view.fxml");

                    parentDelegate.previewFileViewState = new PreviewFileViewState(2,null,qDocsTable.getSelectionModel().getSelectedItem());

                } else {
                    System.out.println("no item is selected to go to preview");
                }

            }
        });
    }
    private void initComboBoxes() {
        loadDates();
        cbProm.getItems().setAll(EnumSet.allOf(PromTypes.class));
        cbProm.getItems().add(0,null);
        cbProm.setCellFactory(listView -> new ListCell<PromTypes>() {
            @Override
            protected void updateItem(PromTypes item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText("All Promitheutes");
                } else {
                    setText(item.name());
                }
            }
        });

        cbProm.valueProperty().addListener(new ChangeListener<PromTypes>() {
            @Override
            public void changed(ObservableValue<? extends PromTypes> observableValue, PromTypes promTypes, PromTypes t1) {
                if(observableValue.getValue()!=null){
                    if(observableValue.getValue().compareTo(PromTypes.AB)==0){
                        cbSuppliers.setDisable(true);
                        cbSuppliers.getSelectionModel().select(0);
                    } else if(observableValue.getValue().compareTo(PromTypes.TRIGONIKOI)==0){
                        cbSuppliers.setDisable(false);
                        cbSuppliers.getSelectionModel().select(0);
                    }
                } else {
                    cbSuppliers.setDisable(true);
                    cbSuppliers.getSelectionModel().select(0);
                }


                //obsFilteredDocs.setAll(filteredDocuments());
            }
        });

        cbType.getItems().setAll(EnumSet.allOf(ABInvoiceTypes.class));
        cbType.getItems().add(0,null);
        cbType.setCellFactory(listView -> new ListCell<ABInvoiceTypes>() {
            @Override
            protected void updateItem(ABInvoiceTypes item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText("All Types");
                } else {
                    setText(item.name());
                }
            }
        });
        cbType.valueProperty().addListener(new ChangeListener<ABInvoiceTypes>() {
            @Override
            public void changed(ObservableValue<? extends ABInvoiceTypes> observableValue, ABInvoiceTypes docTypes, ABInvoiceTypes t1) {
                obsFilteredDocs.setAll(filteredDocuments());
            }
        });


        cbSuppliers.setItems(obsSuppliersOptions);
        SupplierDAO supplierDAO = new SupplierDAO();
        List<Supplier> suppliers = supplierDAO.findAll();
        obsSuppliersOptions.add("ALL");
        for (Supplier sup:suppliers){
            obsSuppliersOptions.add(sup.getName());
        }
        cbSuppliers.getSelectionModel().select(0);
        cbSuppliers.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                obsFilteredDocs.setAll(filteredDocuments());
            }
        });

        qMinDate.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observableValue, LocalDate localDate, LocalDate t1) {
                System.out.println("the new date is: "+t1);
                obsFilteredDocs.setAll(filteredDocuments());
            }
        });

        qMaxDate.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observableValue, LocalDate localDate, LocalDate t1) {
                obsFilteredDocs.setAll(filteredDocuments());
            }
        });

        qMinDate.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return TextExtractions.formatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, TextExtractions.formatter);
                } else {
                    return null;
                }
            }
        });
        qMaxDate.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return TextExtractions.formatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, TextExtractions.formatter);
                } else {
                    return null;
                }
            }
        });




    }
    private ArrayList<Document> filteredDocuments(){
        clearStoreSums();
        BigDecimal temp = new BigDecimal(0);
        ArrayList<Document> filtered = new ArrayList<>();


        if(qMaxDate.valueProperty().get()==null || qMinDate.valueProperty().get()==null){
            System.out.println("a date is null");
            return filtered;
        }

        for (Document doc: dbDocuments){
            if(doc.getDate()==null){
                System.out.println("date is null");
                continue;
            }

            if((storeList.getSelectionModel().getSelectedItems()!=null) && (!storeList.getSelectionModel().getSelectedItems().contains(doc.getStore().getName()))) {
                continue;
            }

            if(cbProm.valueProperty().get()!=null && cbProm.getSelectionModel().getSelectedItem().compareTo(doc.getPromType())!=0){
                continue;
            }

            if(cbType.valueProperty().get()!=null && cbType.getSelectionModel().getSelectedItem().compareTo(doc.getType())!=0){
                continue;
            }

            if(Date.from(qMinDate.valueProperty().get().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).after(doc.getDate())){
                continue;
            }

            if(Date.from(qMaxDate.valueProperty().get().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).before(doc.getDate())) {
                continue;
            }
            if(cbSuppliers.getValue().toLowerCase().compareTo("all")!=0){
                if(doc.getSupplier()==null){
                    continue;
                } else {
                    if(doc.getSupplier().getName().compareTo(cbSuppliers.getValue())!=0){
                        continue;
                    }
                }

            }
            calculateStoreSums(doc);
            filtered.add(doc);
            if(doc.getType().compareTo(ABInvoiceTypes.PISTOTIKO)==0){
                temp = temp.subtract(doc.getSumRetrieved());
            }
            if(doc.getType().compareTo(ABInvoiceTypes.TIMOLOGIO)==0){
                temp = temp.add(doc.getSumRetrieved());
            }
            //
        }
        txtSum.setText(temp+" $");
        filterSumTable.refresh();
        return filtered;
    }
    private void clearStoreSums(){
        for(StoreSummary storeSum:summaryMap.values()){
            storeSum.setABTimologia(BigDecimal.ZERO);
            storeSum.setABPistotika(BigDecimal.ZERO);
            storeSum.setTriTimologia(BigDecimal.ZERO);
            storeSum.setTriPistotika(BigDecimal.ZERO);
        }
    }
    private void initSummaryTable(){
        TableColumn<StoreNames, String> storeNameCol = new TableColumn<>("store");
        storeNameCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            StoreSummary summary = summaryMap.get(store.getName());
            return new SimpleObjectProperty<>(summary.getStoreName());
        });

        TableColumn<StoreNames, BigDecimal> moneySpentColumn = new TableColumn<>("Timologia AB");
        moneySpentColumn.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            StoreSummary summary = summaryMap.get(store.getName());
            return new SimpleObjectProperty<>(summary.getABTimologia());
        });

        TableColumn<StoreNames, BigDecimal> moneyGotColumn = new TableColumn<>("Pistotika AB");
        moneyGotColumn.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            StoreSummary summary = summaryMap.get(store.getName());
            return new SimpleObjectProperty<>(summary.getABPistotika());
        });

        TableColumn<StoreNames, BigDecimal> abDiffCol= new TableColumn<>("AB Diff");
        abDiffCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            BigDecimal spent = summaryMap.get(store.getName()).getABTimologia();
            BigDecimal taken = summaryMap.get(store.getName()).getABPistotika();
            return new SimpleObjectProperty<>(spent.subtract(taken));
        });

        abDiffCol.setCellFactory(column -> new TableCell<StoreNames, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Apply styles directly
                    setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: #333333;");
                }
            }
        });
        abDiffCol.setId("myColumn");
        filterSumTable.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            // This will search for the header of 'myColumn'.
            Node header = filterSumTable.lookup("#myColumn .label");
            if (header != null) {
                header.setStyle("-fx-background-color: #555; -fx-text-fill: #d9d9d9;");
            }
        });

        TableColumn<StoreNames, BigDecimal> timTriCol = new TableColumn<>("Timologia Tri");
        timTriCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            StoreSummary summary = summaryMap.get(store.getName());
            return new SimpleObjectProperty<>(summary.getTriTimologia());
        });

        TableColumn<StoreNames, BigDecimal> pisTimCol = new TableColumn<>("Pistotika Tri");
        pisTimCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            StoreSummary summary = summaryMap.get(store.getName());
            return new SimpleObjectProperty<>(summary.getTriPistotika());
        });

        TableColumn<StoreNames, BigDecimal> triDiffCol = new TableColumn<>("Tri Diff");
        triDiffCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            BigDecimal spent = summaryMap.get(store.getName()).getTriTimologia();
            BigDecimal taken = summaryMap.get(store.getName()).getTriPistotika();
            return new SimpleObjectProperty<>(spent.subtract(taken));
        });

        triDiffCol.setCellFactory(column -> new TableCell<StoreNames, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Apply styles directly
                    setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: #333333;");
                }
            }
        });
        triDiffCol.setId("triDiff");
        filterSumTable.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            // This will search for the header of 'myColumn'.
            Node header = filterSumTable.lookup("#triDiff .label");
            if (header != null) {
                header.setStyle("-fx-background-color: #555; -fx-text-fill: #d9d9d9;");
            }
        });

        TableColumn<StoreNames, BigDecimal> totalCol= new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> {
            StoreNames store = cellData.getValue();
            BigDecimal spent = summaryMap.get(store.getName()).getABTimologia().add(summaryMap.get(store.getName()).getTriTimologia());
            BigDecimal taken = summaryMap.get(store.getName()).getABPistotika().add(summaryMap.get(store.getName()).getTriPistotika());
            return new SimpleObjectProperty<>(spent.subtract(taken));
        });


        ObservableList<StoreNames> stores = FXCollections.observableArrayList(StoreNames.values());




        /*
        StoreNames summary = StoreNames.ALL;

        filterSumTable.setRowFactory(tv -> {
            TableRow<StoreNames> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == summary) { // compare by reference, because 'summary' is a unique instance
                    row.setStyle("-fx-font-weight: bold; -fx-background-color: #d9d9d9;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
         */



        filterSumTable.setItems(stores);
        filterSumTable.getColumns().setAll(storeNameCol,moneySpentColumn,moneyGotColumn,abDiffCol,
                timTriCol,pisTimCol,triDiffCol,totalCol);
    }
    private void calculateStoreSums(Document doc) {
        //System.out.println("entering the function");
        if(doc.getType().compareTo(ABInvoiceTypes.TIMOLOGIO)==0){
            if(doc.getPromType().compareTo(PromTypes.AB)==0){
                summaryMap.get(doc.getStore().getName()).addABTimologia(doc.getSumRetrieved());
                summaryMap.get("All").addABTimologia(doc.getSumRetrieved());
            } else if (doc.getPromType().compareTo(PromTypes.TRIGONIKOI)==0) {
                summaryMap.get(doc.getStore().getName()).addTriTimologia(doc.getSumRetrieved());
                summaryMap.get("All").addTriTimologia(doc.getSumRetrieved());
            }
        } else if (doc.getType().compareTo(ABInvoiceTypes.PISTOTIKO)==0) {
            if(doc.getPromType().compareTo(PromTypes.AB)==0){
                summaryMap.get(doc.getStore().getName()).addABPistotika(doc.getSumRetrieved());
                summaryMap.get("All").addABPistotika(doc.getSumRetrieved());
            } else if (doc.getPromType().compareTo(PromTypes.TRIGONIKOI)==0) {
                summaryMap.get(doc.getStore().getName()).addTriPistotika(doc.getSumRetrieved());
                summaryMap.get("All").addTriPistotika(doc.getSumRetrieved());
            }
        }
    }
    private void initDocQueryTable() {
        TableColumn<Document,String> qDocIdCol = new TableColumn<>("Document Id");
        qDocIdCol.setCellValueFactory(new PropertyValueFactory<>("documentId"));

        TableColumn<Document,String> qDocStore = new TableColumn<>("Store");
        qDocStore.setCellValueFactory(cellData->{
            return new ReadOnlyStringWrapper(cellData.getValue().getStore().getName());
        });

        TableColumn<Document,String> qDocPromTypeCol = new TableColumn<>("Promitheutis");
        qDocPromTypeCol.setCellValueFactory(new PropertyValueFactory<>("promType"));

        TableColumn<Document,String> qDocTypeCol = new TableColumn<>("Type");
        qDocTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Document,String> qDocSum = new TableColumn<>("SUM");
        qDocSum.setCellValueFactory(new PropertyValueFactory<>("sumRetrieved"));

        TableColumn<Document,String> qDocSumCheck = new TableColumn<>("SumCheck");
        qDocSumCheck.setCellValueFactory(new PropertyValueFactory<>("sumEntries"));

        TableColumn<Document,Date> qDocDateCol = new TableColumn<>("Date");
        qDocDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        qDocDateCol.setCellFactory(column -> new TableCell<Document, Date>() {
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
        TableColumn<Document,String> qDocError = new TableColumn<>("has Error");
        qDocError.setCellValueFactory(new PropertyValueFactory<>("hasError"));

        TableColumn<Document,String> qDocSubType = new TableColumn<>("Subtype");
        qDocSubType.setCellValueFactory(new PropertyValueFactory<>("subType"));



        qDocsTable.setItems(obsFilteredDocs);
        qDocsTable.getColumns().setAll(qDocIdCol,qDocStore,
                qDocDateCol,qDocPromTypeCol,qDocTypeCol,qDocSum,
                qDocSumCheck,qDocError,qDocSubType);

        qDocsTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Document>() {
            @Override
            public void changed(ObservableValue<? extends Document> observableValue, Document document, Document t1) {
                if(t1!=null){
                    obsFilteredEntries.setAll(t1.getEntries());
                }

            }
        });
    }
    private void initQEntriesTable() {
        TableColumn<DocEntry,String> qEntryProductId = new TableColumn<>("Product Id");
        qEntryProductId.setCellValueFactory(cellData->{
            DocEntry dc = cellData.getValue();
            //Product p = listManager.getProduct(dc.getProductMaster());
            return new ReadOnlyStringWrapper(dc.getCode());
        });

        TableColumn<DocEntry,String> qEntryDescription= new TableColumn<>("Description");
        qEntryDescription.setCellValueFactory(cellData -> {
            DocEntry entry = cellData.getValue();
            //Product product = listManager.getProduct(entry.getProductMaster());
            return new ReadOnlyStringWrapper(entry.getProduct().getInvDescription());
        });
        //qEntryDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<DocEntry,String> qEntryMaster = new TableColumn<>("master");
        qEntryMaster.setCellValueFactory(new PropertyValueFactory<>("productMaster"));

        //qEntryEan.setCellValueFactory(new PropertyValueFactory<>("lastEan"));

        TableColumn<DocEntry,String> qEntryUnits= new TableColumn<>("Units");
        qEntryUnits.setCellValueFactory(new PropertyValueFactory<>("units"));

        TableColumn<DocEntry,String> qEntryUnitPrice= new TableColumn<>("Unit Price");
        qEntryUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<DocEntry,String> qEntryPrice= new TableColumn<>("Total Price");
        qEntryPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        TableColumn<DocEntry,String> qEntryMixPrice= new TableColumn<>("Mix Price");
        qEntryMixPrice.setCellValueFactory(new PropertyValueFactory<>("mixPrice"));

        //obsFilteredEntries.get(1).

        qEntriesTable.setItems(obsFilteredEntries);
        qEntriesTable.getColumns().setAll(qEntryProductId,qEntryDescription,qEntryMaster,qEntryUnits,qEntryUnitPrice,qEntryPrice,qEntryMixPrice);
    }
    public void loadDates(){
        if(HelloApplication.minDate!=null){
            qMinDate.valueProperty().set(HelloApplication.minDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        if(HelloApplication.maxDate!=null){
            qMaxDate.valueProperty().set(HelloApplication.maxDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
    }
}
