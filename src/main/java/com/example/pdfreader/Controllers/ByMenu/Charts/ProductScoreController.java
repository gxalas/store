package com.example.pdfreader.Controllers.ByMenu.Charts;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.DocEntryDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.DAOs.StoreBasedAttributesDAO;
import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
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
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import javafx.stage.WindowEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class ProductScoreController extends ChildController {
    public Button btnCompareChart;
    private XYChart.Series<String,Number> emptyCompare = new XYChart.Series<>();
    private Map<String, XYChart.Series<String,Number>> compareSeriesMap = new HashMap<>();

    //starting - stuff for the barChart
    // Create axes

    private BarChart<String,Number> compareBarChart;
    private Stage compareStage;
    private ObservableList<XYChart.Series<String,Number>> obsComparingSeries = FXCollections.observableArrayList();
    private Map<String,Stage> popUpMap = new HashMap<>();
    //ending - stuff for the barChart
    public DatePicker dpLastDate;
    @FXML
    public TextField txtDaysBefore = new TextField("7");
    public ComboBox<String> cbDepartment;
    public ComboBox<String> cbStore;
    public TextField txtHope;
    public TextField txtDescription;
    @FXML
    private TableView<StoreBasedAttributes> tableSbas = new TableView<>();
    private ObservableList<StoreBasedAttributes> obsTableSbas = FXCollections.observableArrayList();
    private ObservableList<StoreBasedAttributes> allSbas = FXCollections.observableArrayList();
    @FXML
    public TableView<StoreBasedAttributes> tableCompare = new TableView<>();
    private ObservableList<StoreBasedAttributes> obsTableCompare = FXCollections.observableArrayList();

    private ChangeListener<String> filterSbas = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            filterSbas();
        }
    };
    private EventHandler<ActionEvent> handleTxtChange = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent actionEvent) {
            filterSbas();
        }
    };
    private ChangeListener<String> handleTxtDaysBefore = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            if(t1.isEmpty()){
                return;
            }
            try{
                int d = Integer.parseInt(txtDaysBefore.getText());
                if(d>30){
                    txtDaysBefore.textProperty().removeListener(this);
                    txtDaysBefore.setText(""+30);
                    txtDaysBefore.textProperty().addListener(this);
                } else if(d<0){
                    txtDaysBefore.textProperty().removeListener(this);
                    txtDaysBefore.setText(""+1);
                    txtDaysBefore.textProperty().addListener(this);
                }
            } catch (Exception e){
                txtDaysBefore.textProperty().removeListener(this);
                txtDaysBefore.setText(s);
                txtDaysBefore.textProperty().addListener(this);
            }
        }
    };
    private ChangeListener<Boolean> handleFocusOfTxtDaysBefore = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
            if(!t1){
                if(txtDaysBefore.getText().isEmpty()||Integer.parseInt(txtDaysBefore.getText())==0){
                    txtDaysBefore.textProperty().removeListener(handleTxtDaysBefore);
                    txtDaysBefore.setText("1");
                    txtDaysBefore.textProperty().addListener(handleTxtDaysBefore);
                }
            }
        }
    };
    private ListChangeListener<StoreBasedAttributes> afterLoadedSbas = new ListChangeListener<StoreBasedAttributes>() {
        @Override
        public void onChanged(Change<? extends StoreBasedAttributes> change) {
            obsTableSbas.setAll(allSbas);
            calculateOptions();
        }
    };


    @Override
    public void initialize(HelloController hc) {
        this.parentDelegate = hc;
        Platform.runLater(()->{
            txtDaysBefore.setText("7");
        });
        initDatePicker();

        initTableSbas();
        loadAllStoreBasedAttributes();

        initTableCompare();
        initCompareChart();
    }

    @Override
    public void addMyListeners() {
        txtDaysBefore.textProperty().addListener(handleTxtDaysBefore);
        txtDaysBefore.focusedProperty().addListener(handleFocusOfTxtDaysBefore);
        allSbas.addListener(afterLoadedSbas);
        cbDepartment.getSelectionModel().selectedItemProperty().addListener(filterSbas);
        cbStore.getSelectionModel().selectedItemProperty().addListener(filterSbas);
        txtDescription.addEventHandler(ActionEvent.ACTION,handleTxtChange);
        txtHope.addEventHandler(ActionEvent.ACTION,handleTxtChange);

    }

    @Override
    public void removeListeners(HelloController hc) {
        txtDaysBefore.textProperty().removeListener(handleTxtDaysBefore);
        txtDaysBefore.focusedProperty().removeListener(handleFocusOfTxtDaysBefore);
        allSbas.removeListener(afterLoadedSbas);
        cbDepartment.getSelectionModel().selectedItemProperty().removeListener(filterSbas);
        cbStore.getSelectionModel().selectedItemProperty().removeListener(filterSbas);
        txtDescription.removeEventHandler(ActionEvent.ACTION ,handleTxtChange);
        txtHope.removeEventHandler(ActionEvent.ACTION,handleTxtChange);
    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T) this;
    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }
    private void initTableSbas(){
        TableColumn<StoreBasedAttributes,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            String h = cellData.getValue().getHope();
            return new ReadOnlyStringWrapper(h);
        });

        TableColumn<StoreBasedAttributes,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDepartment();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String s = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(s);
        });

        TableColumn<StoreBasedAttributes,String> productCol = new TableColumn<>("product");
        productCol.setCellValueFactory(cellData->{
            Product p = cellData.getValue().getProduct();
            String pd = "no product";
            if(p!=null){
                pd = p.getInvDescription();
            }
            return new ReadOnlyStringWrapper(pd);
        });

        TableColumn<StoreBasedAttributes, Void> actionCol = new TableColumn<>("Compare");
        actionCol.setCellFactory(param -> new TableCell<StoreBasedAttributes, Void>() {
            private final Button actionBtn = new Button("add");{
                actionBtn.setOnAction((ActionEvent event) -> {
                    StoreBasedAttributes data = getTableView().getItems().get(getIndex());
                    if(obsTableCompare.contains(data)){
                        System.out.println("already exists");
                        return;
                    }
                    Platform.runLater(()->{
                        obsTableCompare.add(data);
                    });


                    // Perform your action here
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBtn);
                }
            }
        });
        tableSbas.getColumns().setAll(storeCol,descriptionCol,hopeCol,departmentCol,productCol,actionCol);
        tableSbas.setRowFactory(tv->{
            TableRow<StoreBasedAttributes> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {

                    if(dpLastDate.getValue()==null){
                        return;
                    }
                    if(txtDaysBefore.getText().trim().isEmpty()){
                        return;
                    }
                    int days = 1;
                    try {
                        days = Integer.parseInt(txtDaysBefore.getText().trim());
                    } catch (Exception e){
                        System.out.println("not a valid string");
                    }
                    if(days<1){
                        return;
                    } else if (days>29){
                        days = 29;
                    }


                    Product product = row.getItem().getProduct();
                    StoreNames store = row.getItem().getStore();
                    String key = product.getInvDescription() + store.getName();

                    LocalDate end = dpLastDate.getValue();
                    LocalDate start = end.minusDays(days);



                    final Stage[] popupStage = {popUpMap.get(key)};

                    if (popupStage[0] == null) {
                        // Load data asynchronously
                        //create the barChart
                        BarChart<String, Number> barChart = getPopupBarChart(product); // Assume getBarChart is defined elsewhere
                        //create the popup
                        popupStage[0] = createPopupStage(product, store,barChart);
                        //put it on the map so we know which pop ups are open
                        popUpMap.put(key, popupStage[0]);
                        popupStage[0].setOnCloseRequest(new EventHandler<WindowEvent>() {
                            @Override
                            public void handle(WindowEvent windowEvent) {
                                System.out.println("it is closing");
                                if(popUpMap.get(key)==null){
                                    System.out.println("the closing doesn't have a map entry");
                                } else {
                                    popUpMap.remove(key);
                                }
                            }
                        });

                        XYChart.Series<String,Number> posEntrySeries = new XYChart.Series<>();
                        posEntrySeries.setName("Sales "+row.getItem().getDescription());

                        XYChart.Series<String,Number> docEntrySeries = new XYChart.Series<>();
                        docEntrySeries.setName("Invoices "+row.getItem().getDescription());

                        XYChart.Series<String,Number> empty = new XYChart.Series<>();
                        docEntrySeries.setName("Invoices "+row.getItem().getDescription());

                        ObservableList<XYChart.Series<String,Number>> obsSeries = FXCollections.observableArrayList();
                        barChart.setData(obsSeries);


                        LocalDate tempDate = start;
                        while (tempDate.isBefore(end)){
                            empty.getData().add(new XYChart.Data<>(tempDate.toString(),0));
                            tempDate = tempDate.plusDays(1);
                        }
                        Platform.runLater(()->{
                            obsSeries.add(posEntrySeries);
                            obsSeries.add(docEntrySeries);
                            obsSeries.add(empty);
                            popupStage[0].show();
                        });

                        getDocEntries(docEntries-> {
                            for(int i=0;i<docEntries.size();i++){
                                if(row.getItem().getStore().compareTo(docEntries.get(i).getDocument().getStore())!=0){
                                    docEntries.remove(docEntries.get(i));
                                }
                            }
                            XYChart.Series<String,Number> docSeries = createDocSeriesData(docEntries,start, end);
                            Platform.runLater(()->{
                                docEntrySeries.setData(docSeries.getData());
                                obsSeries.remove(empty);
                            });
                        },product,start,end);

                        getPosEntries(posEntries-> {
                            Platform.runLater(() -> {
                                posEntrySeries.setData(createPosSeriesData(posEntries,start,end).getData());
                                obsSeries.remove(empty);
                            });
                        },product,store,start,end);
                    }
                }
            });
            return row;
        });
        tableSbas.setItems(obsTableSbas);

    }
    private void loadAllStoreBasedAttributes(){
        MyTask loadSbas = new MyTask(()->{
            StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
            allSbas.setAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());
            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading sbas",
                "fetching all the store based attributes",
                loadSbas
        );
    }
    private void calculateOptions(){
        List<String> deparmtentOptions = new ArrayList<>();
        deparmtentOptions.add("ALL");
        List<String> storeOptions = new ArrayList<>();
        storeOptions.add("ALL");
        allSbas.forEach(sba->{
            if(!deparmtentOptions.contains(sba.getDepartment())){
                deparmtentOptions.add(sba.getDepartment());
            }
            if(!storeOptions.contains(sba.getStore().getName())){
                storeOptions.add(sba.getStore().getName());
            }
        });
        Platform.runLater(()->{
            cbDepartment.getItems().setAll(deparmtentOptions);
            cbStore.getItems().setAll(storeOptions);
            cbDepartment.getSelectionModel().select(0);
            cbStore.getSelectionModel().select(0);
        });

    }
    private void filterSbas(){
        List<StoreBasedAttributes> filtered = new ArrayList<>();
        if(cbStore.getSelectionModel().getSelectedItem()==null){
            System.out.println("null store happened");
            return;
        }
        if(cbDepartment.getSelectionModel().getSelectedItem()==null){
            System.out.println("null department happened");
            return;
        }

        for(int i=0;i<allSbas.size();i++){
            if(!txtDescription.getText().isEmpty()&&!allSbas.get(i).getDescription().toLowerCase().contains(txtDescription.getText().toLowerCase())){
                continue;
            }
            if(!txtHope.getText().isEmpty()&&!allSbas.get(i).getHope().toLowerCase().contains(txtHope.getText().toLowerCase())){
                continue;
            }
            if(cbDepartment.getSelectionModel().getSelectedItem().compareTo("ALL")!=0&&allSbas.get(i).getDepartment().compareTo(cbDepartment.getSelectionModel().getSelectedItem())!=0){
                continue;
            }
            if(cbStore.getSelectionModel().getSelectedItem().compareTo("ALL")!=0 && allSbas.get(i).getStore().getName().compareTo(cbStore.getSelectionModel().getSelectedItem())!=0){
                continue;
            }
            filtered.add(allSbas.get(i));
        }
        Platform.runLater(()->{
            obsTableSbas.setAll(filtered);
        });

    }
    private void getPosEntries(Consumer<List<PosEntry>> onDataLoaded,Product product,StoreNames store,LocalDate start,LocalDate end) {
        MyTask myTask = new MyTask(() -> {
            PosEntryDAO posEntryDAO = new PosEntryDAO();
            Date startDate = HelpingFunctions.convertLocalDateToDate(start);
            Date endDate = HelpingFunctions.convertLocalDateToDate(end);

            List<PosEntry> posEntries = posEntryDAO.findEntriesByProductStoreAndDateRange(product,store,startDate,endDate);
            onDataLoaded.accept(posEntries);  // Trigger the callback with the data
            return null;
        });

        myTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println(workerStateEvent.getSource().getMessage());
                workerStateEvent.getSource().getException().printStackTrace();
            }
        });
        parentDelegate.listManager.addTaskToActiveList(
                "loading posEntries",
                "fetching pos entries "+onDataLoaded,
                myTask
        );
        // Execute the task as before
    }
    private void getDocEntries(Consumer<List<DocEntry>> onDataLoaded,Product product,LocalDate start,LocalDate end){
        MyTask task = new MyTask(()->{
            DocEntryDAO docEntryDAO = new DocEntryDAO();
            List<DocEntry> docEntries = docEntryDAO.findDocEntriesByProductAndDateRange(product.getId(),
                    HelpingFunctions.convertLocalDateToDate(start),HelpingFunctions.convertLocalDateToDate(end));
            onDataLoaded.accept(docEntries);
            return null;
        });

        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                workerStateEvent.getSource().getException().printStackTrace();
                System.out.println(workerStateEvent.getSource().getMessage());
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "retrieving doc entries",
                "doc entry fetching",
                task
        );
    }
    private XYChart.Series<String, Number> createPosSeriesData(List<PosEntry> posEntries, LocalDate start, LocalDate end) {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d"); // For bar labels
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // For keys

        XYChart.Series<String,Number> series = new XYChart.Series<>();

        Map<LocalDate, Number> dateToValueMap = new HashMap<>();
        posEntries.sort(Comparator.comparing(PosEntry::getDate));

        LocalDate tempDate = start;
        int index = 0;
        while (!tempDate.isAfter(end)){
            if(posEntries.size()>index){
                if(posEntries.get(index).getDate().compareTo(HelpingFunctions.convertLocalDateToDate(tempDate))==0){
                    dateToValueMap.put(tempDate, posEntries.get(index).getQuantity());
                    index++;
                }  else {
                    dateToValueMap.put(tempDate, 0);
                }
            } else {
                dateToValueMap.put(tempDate, 0);
            }

            tempDate = tempDate.plusDays(1);
        }
        // Adding entries to the series in the desired order
        dateToValueMap.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Number>comparingByKey()) // Reverse the order if needed
                .forEachOrdered(e -> {
                    String dayLabel = dayFormatter.format(e.getKey())+"\n"+e.getKey().getDayOfWeek();
                    XYChart.Data<String,Number> data = new XYChart.Data<>(dayLabel, e.getValue());
                    series.getData().add(data);
                });

        return series;
    }
    private XYChart.Series<String, Number> createDocSeriesData(List<DocEntry> docEntries,LocalDate start, LocalDate end) {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d"); // For bar labels
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // For keys

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Map<LocalDate, Number> dateToValueMap = new HashMap<>();
        docEntries.sort(Comparator.comparing(docEntry -> docEntry.getDocument().getDate()));

        LocalDate tempDate = start;

        int index = 0;
        while (!tempDate.isAfter(end)){
            if(docEntries.size()>index){
                if(docEntries.get(index).getDate().compareTo(HelpingFunctions.convertLocalDateToDate(tempDate))==0){
                    dateToValueMap.put(tempDate, docEntries.get(index).getUnits());
                    index++;
                }  else {
                    dateToValueMap.put(tempDate, 0);
                }
            } else {
                dateToValueMap.put(tempDate, 0);
            }

            tempDate = tempDate.plusDays(1);
        }
        // Adding entries to the series in the desired order
        dateToValueMap.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Number>comparingByKey()) // Reverse the order if needed
                .forEachOrdered(e -> {
                    String dayLabel = dayFormatter.format(e.getKey())+"\n"+e.getKey().getDayOfWeek();
                    XYChart.Data<String,Number> data = new XYChart.Data<>(dayLabel, e.getValue());
                    series.getData().add(data);
                });

        return series;
    }
    private BarChart<String,Number> getPopupBarChart(Product product){
        CategoryAxis xAxisTemp = new CategoryAxis();
        NumberAxis yAxisTemp = new NumberAxis();
        BarChart<String,Number> barChart = new BarChart<>(xAxisTemp,yAxisTemp);
        xAxisTemp.setLabel("Date");
        yAxisTemp.setLabel("Quantity");
        barChart.setTitle(product.getInvDescription());
        barChart.setBarGap(1);
        barChart.setCategoryGap(50);
        return barChart;
    }
    private Stage createPopupStage(Product product, StoreNames store,Node node) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.NONE);
        popupStage.setTitle("Popup Window: " + product.getInvDescription() + ", " + store);
        // Directly create the layout and scene for the popup
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 10;");
        Scene scene = new Scene(layout); // Create the scene with the initial layout

        layout.setMinWidth(600);
        layout.setMinHeight(400);
        layout.getChildren().add(node);
        popupStage.setScene(scene); // Set the scene on the stage

        // Setup the popup UI components here as needed

        return popupStage;
    }
    private void initTableCompare(){
        TableColumn<StoreBasedAttributes,String> storeCol = new TableColumn<>("store");
        storeCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getStore().getName();
            return new ReadOnlyStringWrapper(d);
        });
        TableColumn<StoreBasedAttributes,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            String d = cellData.getValue().getDescription();
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<StoreBasedAttributes, Void> actionCol = new TableColumn<>("Compare");
        actionCol.setCellFactory(param -> new TableCell<StoreBasedAttributes, Void>() {
            private final Button actionBtn = new Button("remove");{
                actionBtn.setOnAction((ActionEvent event) -> {
                    StoreBasedAttributes data = getTableView().getItems().get(getIndex());
                    obsTableCompare.remove(data);
                    // Perform your action here
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBtn);
                }
            }
        });

        tableCompare.getColumns().setAll(storeCol,descriptionCol,actionCol);
        tableCompare.setItems(obsTableCompare);
        obsTableCompare.addListener(new ListChangeListener<StoreBasedAttributes>() {
            @Override
            public void onChanged(Change<? extends StoreBasedAttributes> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        LocalDate endDate = dpLastDate.getValue();
                        int days = Integer.parseInt(txtDaysBefore.getText().trim());
                        LocalDate startDate = endDate.minusDays(days);

                        getPosEntries((posEntries) -> {
                            Platform.runLater(()->{
                                XYChart.Series<String,Number> posSeriesToAdd = createPosSeriesData(posEntries,startDate,endDate);
                                posSeriesToAdd.setName("("+change.getAddedSubList().get(0).getStore().getName()+") "+change.getAddedSubList().get(0).getDescription());
                                compareSeriesMap.put(change.getAddedSubList().get(0).getProduct().getInvDescription()+change.getAddedSubList().get(0).getStore().getName(),posSeriesToAdd);
                                obsComparingSeries.add(posSeriesToAdd);
                                obsComparingSeries.remove(emptyCompare);
                            });
                        },change.getAddedSubList().get(0).getProduct(),change.getAddedSubList().get(0).getStore(),startDate,endDate);

                        if(!compareStage.isShowing()){
                            compareStage.show();
                        }

                    }
                    if (change.wasRemoved()) {
                        // Logic for handling removed items.
                        System.out.println("Items removed: " + change.getRemoved());

                        String key = change.getRemoved().get(0).getProduct().getInvDescription()+change.getRemoved().get(0).getStore().getName();
                        XYChart.Series<String,Number> seriesToRemove = compareSeriesMap.get(key);
                        Platform.runLater(()->{
                            compareSeriesMap.remove(key);
                            obsComparingSeries.remove(seriesToRemove);
                            if(compareSeriesMap.isEmpty()){
                                obsComparingSeries.add(emptyCompare);
                            }
                        });
                    }
                }
            }
        });
    }
    private void initCompareChart(){
        CategoryAxis xAxisCompare = new CategoryAxis();
        NumberAxis yAxisCompare = new NumberAxis();
        Platform.runLater(()->{
            compareBarChart = new BarChart<>(xAxisCompare,yAxisCompare);
            compareBarChart.setTitle("comparing product sales");
            compareBarChart.setData(obsComparingSeries);
            compareBarChart.getXAxis().setLabel("dates");
            compareBarChart.getYAxis().setLabel("quantity");
        });

        initStage();
        initBtnShowCompare();
    }
    private void initStage(){
        compareStage = new Stage();
        compareStage.initModality(Modality.NONE);
        compareStage.setTitle(" Compare Chart");
        // Directly create the layout and scene for the popup
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 10;");
        Scene scene = new Scene(layout); // Create the scene with the initial layout

        layout.setMinWidth(600);
        layout.setMinHeight(400);
        Platform.runLater(()->{
            layout.getChildren().add(compareBarChart);
            compareStage.setScene(scene); // Set the scene on the stage
        });


        // Setup the popup UI components here as needed

        //return popupStage;
    }
    private void initBtnShowCompare(){
        btnCompareChart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                System.out.println("button compare hit");
                compareStage.show();
            }
        });
    }
    private void initDatePicker(){
        DocumentDAO documentDAO = new DocumentDAO();
        Date docDate = documentDAO.getMaximumDate();
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        Date posDate = posEntryDAO.getMaximumDate();

        if(posDate==null||docDate==null){
            System.out.println("we do not have pos or doc date");
            return;
        }
        Date last = posDate.after(docDate) ? posDate : docDate;
        dpLastDate.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observableValue, LocalDate localDate, LocalDate t1) {
                obsComparingSeries.clear();
                obsTableCompare.clear();
                compareStage.close();
                LocalDate endDate = dpLastDate.getValue();
                int days = Integer.parseInt(txtDaysBefore.getText().trim());
                LocalDate startDate = endDate.minusDays(days);
                emptyCompare.getData().clear();
                int i =0;
                while(startDate.isBefore(endDate)){
                    XYChart.Data<String,Number> data = new XYChart.Data<>(String.valueOf(i) ,0);
                    emptyCompare.getData().add(data);
                    System.out.println("adding data "+emptyCompare.getData().size()+" data : "+data.getXValue());
                    startDate = startDate.plusDays(1);
                    i++;
                }
                Platform.runLater(()->{
                    System.out.println("entries on empty"+emptyCompare.getData().size());
                    obsComparingSeries.add(emptyCompare);
                });
                //compareSeriesMap.clear();
            }
        });

        Platform.runLater(()->{
            dpLastDate.setValue(LocalDate.ofInstant(last.toInstant(),ZoneId.systemDefault()));
        });
    }
}
