package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.DAOs.StoreBasedAttributesDAO;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.PosEntry;
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
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ProductScoreController extends ChildController{
    //starting - stuff for the barChart
    // Create axes
    private CategoryAxis xAxis = new CategoryAxis();
    private NumberAxis yAxis = new NumberAxis();
    @FXML
    private BarChart<String,Number> barChart = new BarChart<>(xAxis,yAxis);
    private XYChart.Series<String, Number> posSeries = new XYChart.Series<>();

    private Map<String,Stage> popUpMap = new HashMap<>();

    //ending - stuff for the barChart
    public DatePicker dpLastDate;
    public TextField txtDaysBefore;
    public ComboBox<String> cbDepartment;
    public ComboBox<String> cbStore;
    public TextField txtHope;
    public TextField txtDescription;

    @FXML
    private TableView<StoreBasedAttributes> tableSbas = new TableView<>();
    private ObservableList<StoreBasedAttributes> obsTableSbas = FXCollections.observableArrayList();
    private ObservableList<StoreBasedAttributes> allSbas = FXCollections.observableArrayList();


    @Override
    public void initialize(HelloController hc) {
        this.parentDelegate = hc;

        initTableSbas();
        initBarChart();
        DocumentDAO documentDAO = new DocumentDAO();
        Date docDate = documentDAO.getMaximumDate();
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        Date posDate = posEntryDAO.getMaximumDate();

        Date last = posDate.after(docDate) ? posDate : docDate;
        System.out.println(last);
        Platform.runLater(()->{
            dpLastDate.setValue(LocalDate.ofInstant(last.toInstant(),ZoneId.systemDefault()));
        });
        txtDaysBefore.setText("7");


        allSbas.addListener(new ListChangeListener<StoreBasedAttributes>() {
            @Override
            public void onChanged(Change<? extends StoreBasedAttributes> change) {
                while (change.next()){
                    if(change.wasAdded()) {
                        System.out.println("the event was triggered " + change.wasReplaced());
                        Platform.runLater(() -> {
                            obsTableSbas.setAll(allSbas);
                        });
                        calculateOptions();
                    }
                }
            }
        });
        loadAllStoreBasedAttributes();
        initDepartmentOptions();
        initStoreOptions();
        initTxtDescription();
        initTxtHope();



    }

    @Override
    public void addMyListeners() {

    }

    @Override
    public void removeListeners(HelloController hc) {
        System.out.println("the size "+popUpMap.size());
        popUpMap.values().forEach(stage->{
            System.out.println("stage "+stage.getTitle());
            stage.close();
        });
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

        tableSbas.setRowFactory(tv->{
            TableRow<StoreBasedAttributes> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    StoreBasedAttributes clickedRow = row.getItem();
                    System.out.println("Double click on: " + clickedRow.getDescription());

                    createPopUp(row.getItem().getProduct(),row.getItem().getStore());
                    // Handle the double-click event here
                }
            });
            return row;
        });



        tableSbas.getColumns().setAll(storeCol,descriptionCol,hopeCol,departmentCol,productCol);
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
    private void initDepartmentOptions(){
        cbDepartment.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                filterSbas();
            }
        });
    }
    private void initStoreOptions(){
        cbStore.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                filterSbas();
            }
        });

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
        obsTableSbas.setAll(filtered);
    }

    private void initTxtDescription(){
        txtDescription.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                filterSbas();
            }
        });
    }

    private void initTxtHope(){
        txtHope.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                filterSbas();
            }
        });
    }
    private List<PosEntry> loadPosEntries(){
        PosEntryDAO posEntryDAO = new PosEntryDAO();

        if(tableSbas.getSelectionModel().getSelectedItem()==null){
            return null;
        }
        if(tableSbas.getSelectionModel().getSelectedItem().getProduct()==null){
            return null;
        }

        if(dpLastDate.getValue()==null){
            return null;
        }
        if(txtDaysBefore.getText().isEmpty()){
            return null;
        }
        int days = 1;
        try {
            days = Integer.parseInt(txtDaysBefore.getText().trim());
        } catch (Exception e){
            System.out.println("not a valid string");
        }
        if(days<1){
            return null;
        }

        Date date = Date.from(dpLastDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Product product = tableSbas.getSelectionModel().getSelectedItem().getProduct();

        List<PosEntry> posEntries = posEntryDAO.findEntriesByProductAndDateRange(product,date,days);
        System.out.println("the posEntries found are "+posEntries.size());

        return posEntries;
    }


    private void initBarChart(){
        xAxis.setLabel("Date");
        yAxis.setLabel("Quantity");
        barChart.setTitle("Data");
        posSeries.setName("Pos Entry");

        // Adjust the bar width and category gap
        barChart.setCategoryGap(5);
        barChart.setBarGap(5);

        barChart.getData().add(posSeries);
    }

    private void
    createPopUp(Product product, StoreNames store){
        String s = product.getInvDescription()+store.getName();
        if(popUpMap.get(s)==null&&product.getInvDescription()!=null&&store.getName()!=null){
            List<PosEntry> tempPosEntries = new ArrayList<>();
            loadPopUpData(tempPosEntries,product,store);
            System.out.println("TEMP POS ENTRIES "+tempPosEntries.size());
        }



    }
    private void initPopUp(Product product,StoreNames store,List<PosEntry> posEntries){
        String s = product.getInvDescription()+store.getName();
        //create the pop up
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.NONE); // Block input events for other windows
        popupStage.setTitle("Popup Window");

        // UI elements for the popup
        Label label = new Label("This is a popup window : "+product.getInvDescription()+", store "+store);
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> popupStage.close());

        VBox layout = new VBox(10, label, closeButton);
        layout.getChildren().add(getBarChart(posEntries,product));
        layout.setStyle("-fx-padding: 10;");

        popUpMap.put(s,popupStage);

        Scene scene = new Scene(layout, 200, 100);
        popupStage.setScene(scene);
        popupStage.showAndWait(); // Show and wait to be closed
    }

    private void loadPopUpData(List<PosEntry> posEntryTempResult,Product product,StoreNames store){
        AtomicReference<List<PosEntry>> posList = new AtomicReference<>(new ArrayList<>());
        MyTask myTask = new MyTask(()->{
            List<PosEntry> result = loadPosEntries();
            if(result==null){
                System.out.println("nothing found");
            } else {
                posList.set(result);
            }
            posEntryTempResult.clear();
            posEntryTempResult.addAll(posList.get());
            return null;
        });

        myTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println("Succeded "+posEntryTempResult.size()+" !!!!!!! !!!!");
                System.out.println(product.getInvDescription());
                initPopUp(product,store,posEntryTempResult);
            }
        });
        myTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                workerStateEvent.getSource().getException().printStackTrace();
                System.out.println("error");
                System.out.println(workerStateEvent.getSource().getMessage());
            }
        });
        parentDelegate.listManager.addTaskToActiveList(
                "loading pos",
                "fetching pos",
                myTask
        );
    }

    private BarChart<String,Number> getBarChart(List<PosEntry>posEntries,Product product){
        CategoryAxis xAxisTemp = new CategoryAxis();
        NumberAxis yAxisTemp = new NumberAxis();
        BarChart<String,Number> barChart = new BarChart<>(xAxisTemp,yAxisTemp);

        XYChart.Series<String,Number> tempPosSeries = getPosSeries(posEntries,product);



        xAxisTemp.setLabel("Date");
        yAxisTemp.setLabel("Quantity");
        barChart.setTitle(product.getInvDescription());


        // Adjust the bar width and category gap
        barChart.setCategoryGap(5);
        barChart.setBarGap(5);

        barChart.getData().add(tempPosSeries);
        return barChart;
    }
    private XYChart.Series<String,Number> getPosSeries(List<PosEntry> posEntries,Product product){
        // Populate the series
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        XYChart.Series<String, Number> tempPosSeries = new XYChart.Series<>();
        tempPosSeries.setName("Sales "+product.getInvDescription());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");

        tempPosSeries.getData().clear();
        for (PosEntry posEntry : posEntries) {
            String dateLabel = dateFormat.format(posEntry.getDate());
            XYChart.Data<String,Number> data = new XYChart.Data<>(dateLabel, posEntry.getQuantity());
            tempPosSeries.getData().add(data);
            StackPane bar = (StackPane) data.getNode();
            if(bar!=null)
            bar.setScaleX(0.1);
        }
        return  tempPosSeries;
    }

}
