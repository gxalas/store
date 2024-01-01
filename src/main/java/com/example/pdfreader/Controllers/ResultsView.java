package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SaleSummary;
import com.example.pdfreader.MyCustomEvents.Example.CustomEvent;
import com.example.pdfreader.MyCustomEvents.Example.CustomEventListener;
import com.example.pdfreader.MyCustomEvents.Example.EventManager;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ResultsView extends ChildController{
    public TableView<SaleSummary> tableResults;
    public Button btnToggle;
    @FXML
    private TableView<String> tableNullDates;
    private ObservableList<String> obsTextNullDates = FXCollections.observableArrayList();

    public LineChart<Number, Number> myLineChart;
    private ObservableList<SaleSummary> obsSaleSummaries = FXCollections.observableArrayList();
    private ArrayList<Document> documentsRetrieved = new ArrayList<>();
    private NumberAxis xAxis; // Assume this is a class member now
    private NumberAxis yAxis; // Assume this is a class member now
    private ObservableList<XYChart.Series<Number,Number>> obsSeries = FXCollections.observableArrayList();
    private XYChart.Series<Number, Number> seriesInvoices = new XYChart.Series<>(); //2
    private XYChart.Series<Number, Number> seriesPosSales = new XYChart.Series<>();
    private XYChart.Series<Number, Number> seriesRunningAverage = new XYChart.Series<>(); //2
    private XYChart.Series<Number,Number> seriesInvoicrsRunningAverage = new XYChart.Series<>();
    private IntegerProperty taskCounter = new SimpleIntegerProperty();
    private Date minPosDate;
    private Date maxPosDate;
    private Date minDocDate;
    private Date maxDocDate;
    public ComboBox<Integer> cbYear;
    public ComboBox<String> cbStore;
    private final ObservableList<String> obsStores = FXCollections.observableArrayList();
    private final ObservableList<Integer> obsYears = FXCollections.observableArrayList();
    private Long referenceTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private double lastXMousePosition = 0.0;
    private double lastYMousePosition = 0.0;
    private double xMousePosition = 0.0;
    private double yMousePosition = 0.0;
    private Tooltip lastTooltip = new Tooltip();

    //----- new attempt

    private IntegerProperty maxMinPosEntriesCounter = new SimpleIntegerProperty();
    private IntegerProperty maxMinDocumentCounter = new SimpleIntegerProperty();
    private IntegerProperty maxMinCounter = new SimpleIntegerProperty();



    private final CustomEventListener cel = new CustomEventListener() {
        @Override
        public void onStarting(CustomEvent event) {
            System.out.println("from the results view \n the message: "+event.getMessage());
        }
        @Override
        public void onEnding(CustomEvent event){
            event.setEndTime(System.nanoTime());
            System.out.println("it took : "+(event.getEndTime()-event.getStartTime())/1_000_000.00+" milliseconds");
        }
    };
    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        initCbs();
        initTableResults();
        initLineChart();
        initZoomingListeners();
        addLineChartListener();

        TableColumn<String,String> textCol = new TableColumn<>("text");
        textCol.setCellValueFactory(celData->{
            return new ReadOnlyStringWrapper(celData.getValue());
        });
        tableNullDates.setItems(obsTextNullDates);
        tableNullDates.getColumns().setAll(textCol);


        addMaxMinListeners();
        startGettingDates();

        // Adjust thread pool size as needed

    }

    private void addMaxMinListeners(){
        maxMinPosEntriesCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (t1.intValue()==2){
                    maxMinPosEntriesCounter.set(0);
                    maxMinCounter.set(maxMinCounter.intValue()+1);
                    //add method for fetching
                }
            }
        });

        maxMinDocumentCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(t1.intValue()==2){
                    maxMinDocumentCounter.set(0);
                    maxMinCounter.set(maxMinCounter.intValue()+1);
                }
            }
        });

        maxMinCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(maxMinCounter.intValue()==2){
                    maxMinCounter.set(0);
                    //set the values for the combo boxes
                    setObsYears();
                    setObsStores();
                    Platform.runLater(()->{
                        getTheDataFromDB();
                    });

                }
            }
        });
    }
    private void startGettingDates(){
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        MyTask getMinPosDate = new MyTask(()->{
            minPosDate = posEntryDAO.getMinimumDate();
            System.out.println("the min pos date: "+minPosDate);
            return null;
        });
        getMinPosDate.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                maxMinPosEntriesCounter.set(maxMinPosEntriesCounter.intValue()+1);
            }
        });

        MyTask getMaxPosDate = new MyTask(()->{
            maxPosDate = posEntryDAO.getMaximumDate();
            System.out.println("the max pos date: "+maxPosDate);
            return null;
        });
        getMaxPosDate.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                maxMinPosEntriesCounter.set(maxMinPosEntriesCounter.intValue()+1);
            }
        });

        DocumentDAO documentDAO = new DocumentDAO();

        MyTask getMinDocDate = new MyTask(()->{
            minDocDate = documentDAO.getMinimumDate();
            return null;
        });
        getMinDocDate.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                maxMinDocumentCounter.set(maxMinDocumentCounter.intValue()+1);
            }
        });

        MyTask getMaxDocDate = new MyTask(()->{
            maxDocDate = documentDAO.getMaximumDate();
            return null;
        });
        getMaxDocDate.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                maxMinDocumentCounter.set(maxMinDocumentCounter.intValue()+1);
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "get Min Pos",
                "retrieve min pos date",
                getMinPosDate
        );
        parentDelegate.listManager.addTaskToActiveList(
                "get Max Pos",
                "retrieve max pos date",
                getMaxPosDate
        );
        parentDelegate.listManager.addTaskToActiveList(
                "get Min Doc",
                "retrieve min Doc Date",
                getMinDocDate
        );
        parentDelegate.listManager.addTaskToActiveList(
                "get Max Doc",
                "retrieve max Doc Date",
                getMaxDocDate
        );

    }


    @Override
    public void addMyListeners() {
        EventManager.getInstance().addEventListener(cel);

    }

    @Override
    public void removeListeners(HelloController hc) {
        EventManager.getInstance().removeEventListener(cel);
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
    when both tasks have ended we begin calculating the series
     */
    private void addLineChartListener() {
        taskCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                System.out.println("the value is "+t1);
                if(t1.intValue()==2){
                    cbStore.setDisable(false);
                    cbYear.setDisable(false);
                    calculatePosAndRunningAvgSeries();
                    calculateInvoicesSeries();
                    myLineChart.layout();
                } else {
                    cbStore.setDisable(true);
                    cbYear.setDisable(true);
                }

            }
        });
    }
    private void makePosSalesVisible(){
        seriesPosSales.getNode().setVisible(true);
        for(XYChart.Data<Number,Number> data : seriesPosSales.getData()){
            data.getNode().setVisible(true);
        }

    }
    private void initZoomingListeners() {
        btnToggle.setOnAction(e -> {
            if (seriesPosSales.getNode().visibleProperty().get()){
                seriesPosSales.getNode().setVisible(false);
                seriesInvoices.getNode().setVisible(false);
                for(XYChart.Data<Number,Number> data : seriesPosSales.getData()){
                    data.getNode().setVisible(false);
                }
                for(XYChart.Data<Number,Number> data : seriesInvoices.getData()){
                    data.getNode().setVisible(false);
                }
            } else {
                seriesPosSales.getNode().setVisible(true);
                seriesInvoices.getNode().setVisible(true);
                for(XYChart.Data<Number,Number> data : seriesPosSales.getData()){
                    data.getNode().setVisible(true);
                }
                for(XYChart.Data<Number,Number> data : seriesInvoices.getData()){
                    data.getNode().setVisible(true);
                }
            }



            //zoom(0.9); // Zoom in (show more detail)
            //System.out.println("zoom in");
        });
        myLineChart.setOnScroll(e -> {
            if (e.getDeltaY() == 0) {
                return;
            }

            double scaleFactor = (e.getDeltaY() > 0) ? 1.1 : 0.9;

            zoom(scaleFactor);
        });
        myLineChart.setOnMousePressed(mouseEvent -> {
            xMousePosition = mouseEvent.getX();
            yMousePosition = mouseEvent.getY();
            lastXMousePosition = xMousePosition;
            lastYMousePosition = yMousePosition;
        });
        myLineChart.setOnMouseDragged(mouseEvent -> {
            double xTranslationDistance = mouseEvent.getX() - lastXMousePosition;
            double yTranslationDistance = mouseEvent.getY() - lastYMousePosition;
            lastXMousePosition = mouseEvent.getX();
            lastYMousePosition = mouseEvent.getY();

            double xAxisScaleFactor = getScaleFactorForXAxis();
            double yAxisScaleFactor = getScaleFactorForYAxis(); // Implement this method

            double deltaX = xTranslationDistance * xAxisScaleFactor;
            double deltaY = yTranslationDistance * yAxisScaleFactor;

            xAxis.setLowerBound(xAxis.getLowerBound() - deltaX);
            xAxis.setUpperBound(xAxis.getUpperBound() - deltaX);

            yAxis.setLowerBound(yAxis.getLowerBound() + deltaY); // Notice the + sign for Y-axis
            yAxis.setUpperBound(yAxis.getUpperBound() + deltaY);
        });
    }
    private MyTask getPosEntriesFromDb() {
        return new MyTask(()->{
            PosEntryDAO posEntryDAO = new PosEntryDAO();
            //List<PosEntry> posEntryList = posEntryDAO.getAllPosEntries();
            int year = cbYear.getValue();
            String store = cbStore.getValue();


            List<PosEntry> posEntryList = posEntryDAO.getPosEntriesByYearAndStore(year,store);


            HashMap<Date, SaleSummary> salesMap = new HashMap<>();

            for(PosEntry pos : posEntryList){
                if(salesMap.get(pos.getDate())==null){
                    salesMap.put(pos.getDate(),new SaleSummary(pos.getDate()));
                }
                salesMap.get(pos.getDate()).addValueToSum(pos.money);
            }
            Platform.runLater(() -> {
                obsSaleSummaries.setAll(salesMap.values());
            });
            return null;
        });
    }
    private MyTask getInvoicesFromDB(){
        return new MyTask(()->{
            CustomEvent event = new CustomEvent("loading invoices");
            EventManager.getInstance().fireStarting(event);


            DocumentDAO documentDAO = new DocumentDAO();
            //List<Document> documents =
            int year = cbYear.getValue();
            String store = cbStore.getValue();

            documentsRetrieved = new ArrayList<>(documentDAO.getDocumentsByYearAndStore(year,store));
            System.out.println("the docs retrieved count is "+documentsRetrieved.size());
            documentsRetrieved = (ArrayList<Document>) documentsRetrieved.stream().
                    sorted(Comparator.comparing(Document::getDate)).
                    collect(Collectors.toList());

            if(!documentsRetrieved.isEmpty()){
                SaleSummary refSum = new SaleSummary(documentsRetrieved.get(0).getDate());
                List<SaleSummary> testList = new ArrayList<>();
                testList.add(refSum);
                //updateReferenceTimestamp(testList);
            }

            EventManager.getInstance().fireEnding(event);
            return null;
        });
    }
    private void calculateInvoicesSeries(){
        int a =0;
        Date currentDate = new Date();
        Date previousDate = new Date();
        BigDecimal dateSum = new BigDecimal(0);

        //calculating the date sums
        XYChart.Series<Number, Number> tempSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> tempRunSeries = new XYChart.Series<>();

        for (Document document:documentsRetrieved){
            currentDate = document.getDate();
            if(a==0 || currentDate.compareTo(previousDate)==0){
                dateSum = dateSum.add(document.getSumRetrieved());
                //System.out.println(document.getSumRetrieved()+" "+dateSum);
            } else {
                long numericDate = dateToNumeric(document.getDate());
                XYChart.Data<Number, Number> dataAvg = new XYChart.Data<>(numericDate,dateSum);
                tempSeries.getData().add(dataAvg);
                //seriesInvoices.getData().add(dataAvg);
                dateSum =BigDecimal.ZERO;
            }
            previousDate = document.getDate();
            a++;
        }
        if(tempSeries.getData().isEmpty()){
            Calendar calMin = Calendar.getInstance();
            calMin.set(cbYear.getValue(),Calendar.JANUARY,1);
            tempSeries.getData().add(new XYChart.Data<>(dateToNumeric(calMin.getTime()),0));
        }

        if(tempSeries.getData().size()>10){
            Queue<BigDecimal> fifoQueue = new LinkedList<>();
            for (XYChart.Data<Number,Number> sumi : tempSeries.getData()){
                //checking if we are missing a date

                Number numericDate = sumi.getXValue();
                fifoQueue.add(BigDecimal.valueOf(sumi.getYValue().longValue()));

                if(fifoQueue.size()>9){
                    BigDecimal avg = fifoQueue.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    XYChart.Data<Number, Number> dataAvg = new XYChart.Data<>(numericDate, avg.divide(BigDecimal.TEN, RoundingMode.HALF_UP));
                    tempRunSeries.getData().add(dataAvg);
                    fifoQueue.poll();
                }

                // Attach an event handler to the data point
                // Inside the listener for the node property
                // Add data to series with event handlers
                //addListenerToPoint(data);

                //tempPos.getData().add(data);
                //seriesPosSales.getData().add(data);
            }
            //updateReferenceTimestamp(sortedSummaries);
        } else {
            Calendar calD = Calendar.getInstance();
            calD.set(cbYear.getValue(),Calendar.JANUARY,0);
            //adding default values for series not being empty
            long numericDate = dateToNumeric(calD.getTime());
            tempRunSeries.getData().add(new XYChart.Data<Number,Number>(numericDate,0));
        }

        tempSeries.getData().forEach(this::addListenerToPoint);

        //documentsRetrieved.clear();

        Platform.runLater(() -> {
            seriesInvoices.setName("Invoices "+cbStore.getValue()+" "+cbYear.getValue()); //2
            seriesInvoices.setData(tempSeries.getData());
            seriesInvoicrsRunningAverage.setData(tempRunSeries.getData());
            seriesInvoicrsRunningAverage.setName("Invoices Running Average "+cbStore.getValue()+" "+cbYear.getValue());

            System.out.println("\n\n\nthe series invoices "+seriesInvoices.getData().size()+" \n\n\n"+numericToDate(seriesInvoices.getData().get(0).getXValue().longValue()));
            myLineChart.layout();
        });

    }
    private void calculatePosAndRunningAvgSeries() {
        XYChart.Series<Number,Number> tempPos = new XYChart.Series<>();
        XYChart.Series<Number,Number> tempRun = new XYChart.Series<>();
        obsTextNullDates.clear();


        // Here you need to watch out because sorting might be needed
        //we are getting the results from the table
        List<SaleSummary> sortedSummaries = new ArrayList<>(obsSaleSummaries);
        sortedSummaries.sort(Comparator.comparing(SaleSummary::getDate));

        if (sortedSummaries.isEmpty()){
            //adding default values for series not being empty
            long numericDate = dateToNumeric(minPosDate);
            tempPos.getData().add(new XYChart.Data<Number,Number>(numericDate,0));
            tempRun.getData().add(new XYChart.Data<Number,Number>(numericDate,0));
        } else {
            //a queue for holding the values of the running average
            Queue<BigDecimal> fifoQueue = new LinkedList<>();
            //updateReferenceTimestamp(sortedSummaries);
            LocalDate lastDate = sortedSummaries.get(0).getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            //LocalDate date1 = myDate1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            for (SaleSummary sumi : sortedSummaries){
                //checking if we are missing a date
                LocalDate sumiDate = sumi.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(lastDate, sumiDate);
                if (daysBetween >1){
                    System.out.println("We are missing a date !!! - - - - - - - - --- - - "+sumi.getDate());
                    Platform.runLater(()->{
                        obsTextNullDates.add("we are missing "+sumi.getDate());
                    });

                }
                lastDate = sumiDate;

                long numericDate = dateToNumeric(sumi.getDate());
                XYChart.Data<Number, Number> data = new XYChart.Data<>(numericDate, sumi.getSum());
                fifoQueue.add(sumi.getSum());

                if(fifoQueue.size()>9){
                    BigDecimal avg = fifoQueue.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    XYChart.Data<Number, Number> dataAvg = new XYChart.Data<>(numericDate, avg.divide(BigDecimal.TEN, RoundingMode.HALF_UP));
                    tempRun.getData().add(dataAvg);
                    fifoQueue.poll();
                }

                // Attach an event handler to the data point
                // Inside the listener for the node property
                // Add data to series with event handlers
                addListenerToPoint(data);

                tempPos.getData().add(data);
                //seriesPosSales.getData().add(data);
            }
            if(tempRun.getData().isEmpty()){
                Calendar calD = Calendar.getInstance();
                calD.set(cbYear.getValue(),Calendar.JANUARY,0);
                //adding default values for series not being empty
                long numericDate = dateToNumeric(calD.getTime());
                tempRun.getData().add(new XYChart.Data<Number,Number>(numericDate,0));
            }
        }

        Platform.runLater(()->{
            seriesPosSales.setName("Sales Data "+cbStore.getValue()+" "+cbYear.getValue());
            seriesPosSales.setData(tempPos.getData());

            seriesRunningAverage.setName("Sales Running Average "+cbStore.getValue()+" "+cbYear.getValue()); //2
            //seriesInvoicrsRunningAverage.setName("Invoices Running Average "+cbStore.getValue()+" "+cbYear.getValue());
            seriesRunningAverage.setData(tempRun.getData());

            myLineChart.layout();
        });

    }
    private void addListenerToPoint(XYChart.Data<Number,Number> data){
        data.nodeProperty().addListener((observable, oldValue, newNode) -> {
            if (newNode != null) {
                newNode.setOnMouseEntered(mouseEvent -> {
                    // Hide the last tooltip before showing a new one
                    if (lastTooltip.isShowing()) {
                        lastTooltip.hide();
                    }

                    Date date = numericToDate(data.getXValue().longValue());
                    Number value = data.getYValue();
                    lastTooltip = new Tooltip("Date: " + dateFormat.format(date) + "\nValue: " + value);

                    // Install and show the tooltip near the cursor
                    Tooltip.install(newNode, lastTooltip);
                    lastTooltip.show(newNode, mouseEvent.getScreenX(), mouseEvent.getScreenY() + 15);
                });

                newNode.setOnMouseExited(mouseEvent -> {
                    // Hide and uninstall the tooltip when the mouse leaves the node
                    if (lastTooltip.isShowing()) {
                        lastTooltip.hide();
                    }
                    Tooltip.uninstall(newNode, lastTooltip);
                });

                // Add a listener to the scene property of the node
                newNode.sceneProperty().addListener((sceneObservable, oldScene, newScene) -> {
                    if (newScene != null) {
                        // Add event filter to the new scene
                        newScene.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                            if (lastTooltip != null && lastTooltip.isShowing()) {
                                lastTooltip.hide();
                            }
                        });
                    }
                });
            }
        });
    }
    public void initLineChart() {
        xAxis = (NumberAxis) myLineChart.getXAxis();
        yAxis = (NumberAxis) myLineChart.getYAxis();
        myLineChart.setTitle("Sale Summary Over Time");
        myLineChart.getXAxis().setLabel("Date");
        myLineChart.getYAxis().setLabel("Sale Summary Value");
        myLineChart.getXAxis().setAutoRanging(false);
        myLineChart.getYAxis().setAutoRanging(false);
        //myLineChart.getYAxis().rang

        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (referenceTimestamp == null) return "";
                return dateFormat.format(new Date(referenceTimestamp + TimeUnit.DAYS.toMillis(object.longValue())));
            }

            @Override
            public Number fromString(String string) {
                try {
                    return TimeUnit.MILLISECONDS.toDays(dateFormat.parse(string).getTime() - referenceTimestamp);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        yAxis.setUpperBound(10000);
        yAxis.setTickUnit(1000);

        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();
        cb.add(Calendar.DATE,20);

        long diff = cb.getTimeInMillis() - ca.getTimeInMillis();
        yAxis.setTickUnit(diff);


        myLineChart.setData(obsSeries);


        obsSeries.addAll(seriesPosSales,seriesRunningAverage,seriesInvoices,seriesInvoicrsRunningAverage);
    }
    private double getScaleFactorForXAxis() {
        // Get the width of the chart in pixels
        double axisLength = myLineChart.getWidth();
        // Get the range of data displayed on the axis
        double range = xAxis.getUpperBound() - xAxis.getLowerBound();
        // Calculate how much data one pixel represents
        return range / axisLength;
    }
    private double getScaleFactorForYAxis() {
        // Get the width of the chart in pixels
        double axisLength = myLineChart.getHeight();
        // Get the range of data displayed on the axis
        double range = yAxis.getUpperBound() - yAxis.getLowerBound();
        // Calculate how much data one pixel represents
        return range / axisLength;
    }
    private void updateReferenceTimestamp() {
        Calendar calRef = Calendar.getInstance();
        calRef.set(cbYear.getValue(),Calendar.JANUARY,0);
        //SaleSummary earliestSummary = sortedSummaries.get(0); // Assuming sortedSummaries is sorted by date
        //referenceTimestamp = earliestSummary.getDate().getTime();
        referenceTimestamp = calRef.getTime().getTime();
    }


    private void zoom(double scaleFactor) {
        if (referenceTimestamp == null) {
            return; // Safety check to make sure we have referenceTimestamp set
        }

        // Determine the current range for the x-axis
        double xAxisLowerBound = xAxis.getLowerBound();
        double xAxisUpperBound = xAxis.getUpperBound();
        double xAxisRange = xAxisUpperBound - xAxisLowerBound;
        double xAxisMidpoint = xAxisLowerBound + (xAxisRange / 2.0);

        // Calculate the new bounds based on the zoom scale factor
        double newRange = xAxisRange * scaleFactor;
        double newLowerBound = xAxisMidpoint - (newRange / 2.0);
        double newUpperBound = xAxisMidpoint + (newRange / 2.0);

        // Update the x-axis bounds using the calculated values
        xAxis.setLowerBound(newLowerBound);
        xAxis.setUpperBound(newUpperBound);

        // Request layout to take effect of changes
        myLineChart.requestLayout();
    }

    private long dateToNumeric(Date date) {
        if (referenceTimestamp == null) return 0;
        long diffInMillies = date.getTime() - referenceTimestamp;
        return TimeUnit.MILLISECONDS.toDays(diffInMillies);
    }
    private Date numericToDate(long numeric) {
        long millisSinceReference = TimeUnit.DAYS.toMillis(numeric);
        return new Date(referenceTimestamp + millisSinceReference);
    }
    public void initTableResults(){
        TableColumn<SaleSummary,Date> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData->{
            Date date = cellData.getValue().getDate();
            return new ReadOnlyObjectWrapper<>(date);
        });
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

        TableColumn<SaleSummary,Number> moneyCol = new TableColumn<>("Money");
        moneyCol.setCellValueFactory(new PropertyValueFactory<>("sum"));


        tableResults.getColumns().setAll(dateCol,moneyCol);
        tableResults.setItems(obsSaleSummaries);
    }
    /*
    initializing the combo boxes
    and retrieving the initial data
     */
    private void initCbs() {

        cbStore.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if(taskCounter.get()==2){
                    getTheDataFromDB();
                }
            }
        });
        cbYear.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observableValue, Integer integer, Integer t1) {
                if(taskCounter.get()==2){
                    getTheDataFromDB();
                }
            }
        });
        taskCounter.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (taskCounter.intValue()==2){
                    updateReferenceTimestamp();
                    Calendar cStart = Calendar.getInstance();
                    cStart.set(cbYear.getValue(),Calendar.JANUARY,0);
                    Calendar cEnd = Calendar.getInstance();
                    cEnd.set(cbYear.getValue(),Calendar.DECEMBER,30);
                    Platform.runLater(()->{
                        myLineChart.getXAxis().setAutoRanging(false);
                        xAxis.setLowerBound(dateToNumeric(cStart.getTime()) );
                        xAxis.setUpperBound(dateToNumeric(cEnd.getTime()));
                        myLineChart.requestLayout();
                        makePosSalesVisible();
                    });
                }
            }
        });


        cbYear.setItems(obsYears);
        cbStore.setItems(obsStores);
        taskCounter.set(0);
    }
    public void getTheDataFromDB(){
        taskCounter.set(0);
        MyTask loadPosEntries = getPosEntriesFromDb();
        MyTask loadDocuments = getInvoicesFromDB();
        loadDocuments.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Throwable exception = workerStateEvent.getSource().getException();
                if (exception != null) {
                    System.out.println("Task failed due to: " + exception.getMessage());
                    exception.printStackTrace(); // For detailed error trace
                } else {
                    System.out.println("Task failed for an unknown reason.");
                }

            }
        });


        loadPosEntries.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                taskCounter.set(taskCounter.get()+1);
            }
        });

        loadDocuments.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                taskCounter.set(taskCounter.get()+1);
            }
        });

        parentDelegate.listManager.addTaskToActiveList(
                "loading Pos Entries",
                "fetching all pos entries from db",
                loadPosEntries
        );
        parentDelegate.listManager.addTaskToActiveList(
                "loading Documents",
                "fetching all the documents from the database",
                loadDocuments
        );

    }
    public void setObsYears(){
        System.out.println("setting years");
        obsYears.clear();
        boolean posDates = true;
        boolean invDates = true;
        Date minDate, maxDate;
        if (minPosDate == null || maxPosDate ==null){
            System.out.println(" coundn't get min and max dates for pos entries");
            posDates = false;
        }
        if (maxDocDate == null || minDocDate ==null){
            System.out.println(" coundn't get min and max dates for documents");
            invDates = false;
        }
        if(posDates!=invDates){
            if(posDates){
                minDate = minPosDate;
                maxDate = maxPosDate;
            } else {
                minDate = minDocDate;
                maxDate = maxDocDate;
            }
        } else

        if(!posDates){

            return;
        } else {
            if(minDocDate.before(minPosDate)){
                minDate = minDocDate;
            } else {
                minDate = minPosDate;
            }

            if(maxDocDate.before(maxPosDate)){
                maxDate = maxDocDate;
            } else {
                maxDate = maxPosDate;
            }
        }

        Calendar calMin = Calendar.getInstance();
        Calendar calMax = Calendar.getInstance();
        calMin.setTime(minDate);
        calMax.setTime(maxDate);
        ArrayList<Integer> choices =  new ArrayList<>();

        int minYear = calMin.get(Calendar.YEAR);
        int maxYear = calMax.get(Calendar.YEAR);
        System.out.println("min year "+minYear+", max year "+maxYear);
        for (int i=minYear;i<maxYear+1;i++){
            choices.add(i);
        }
        Platform.runLater(()->{
            obsYears.setAll(choices);
            cbYear.getSelectionModel().select(0);
        });
    }
    public void setObsStores (){
        obsStores.setAll(StoreNames.stringValues());
        if(!obsStores.isEmpty()){
            cbStore.getSelectionModel().select(0);
        }
    }
}
