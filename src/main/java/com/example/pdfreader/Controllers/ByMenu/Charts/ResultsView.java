package com.example.pdfreader.Controllers.ByMenu.Charts;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SaleSummary;
import com.example.pdfreader.MyCustomEvents.Example.CustomEvent;
import com.example.pdfreader.MyCustomEvents.Example.CustomEventListener;
import com.example.pdfreader.MyCustomEvents.Example.EventManager;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
import com.example.pdfreader.Sinartiseis.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ResultsView extends ChildController {
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
    private final ChangeListener<String> handleStoreChange = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            getTheDataFromDB();
        }
    };
    private final ChangeListener<Integer> handleYearChange = new ChangeListener<Integer>() {
        @Override
        public void changed(ObservableValue<? extends Integer> observableValue, Integer integer, Integer t1) {
            getTheDataFromDB();
        }
    };
    private final EventHandler<MouseEvent> mousePressedOnLineChart = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            xMousePosition = mouseEvent.getX();
            yMousePosition = mouseEvent.getY();
            lastXMousePosition = xMousePosition;
            lastYMousePosition = yMousePosition;
        }
    };
    private final EventHandler<MouseEvent> onMouseDraggedOnLineChart = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            double xTranslationDistance = mouseEvent.getX() - lastXMousePosition;
            double yTranslationDistance = mouseEvent.getY() - lastYMousePosition;
            lastXMousePosition = mouseEvent.getX();
            lastYMousePosition = mouseEvent.getY();

            double xAxisScaleFactor = ResultsView.this.getScaleFactorForXAxis();
            double yAxisScaleFactor = ResultsView.this.getScaleFactorForYAxis(); // Implement this method

            double deltaX = xTranslationDistance * xAxisScaleFactor;
            double deltaY = yTranslationDistance * yAxisScaleFactor;

            xAxis.setLowerBound(xAxis.getLowerBound() - deltaX);
            xAxis.setUpperBound(xAxis.getUpperBound() - deltaX);

            yAxis.setLowerBound(yAxis.getLowerBound() + deltaY); // Notice the + sign for Y-axis
            yAxis.setUpperBound(yAxis.getUpperBound() + deltaY);
        }
    };
    private final EventHandler<ScrollEvent> scrollHandleOnLineChart = new EventHandler<ScrollEvent>() {
        @Override
        public void handle(ScrollEvent scrollEvent) {
            if (scrollEvent.getDeltaY() == 0) {
                return;
            }
            double scaleFactor = (scrollEvent.getDeltaY() > 0) ? 1.1 : 0.9;
            ResultsView.this.zoom(scaleFactor);
        }
    };
    private final EventHandler<ActionEvent> btnToggleHandle = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent actionEvent) {
            if (seriesPosSales.getNode().visibleProperty().get()) {
                seriesPosSales.getNode().setVisible(false);
                seriesInvoices.getNode().setVisible(false);
                for (XYChart.Data<Number, Number> data : seriesPosSales.getData()) {
                    data.getNode().setVisible(false);
                }
                for (XYChart.Data<Number, Number> data : seriesInvoices.getData()) {
                    data.getNode().setVisible(false);
                }
            } else {
                seriesPosSales.getNode().setVisible(true);
                seriesInvoices.getNode().setVisible(true);
                for (XYChart.Data<Number, Number> data : seriesPosSales.getData()) {
                    data.getNode().setVisible(true);
                }
                for (XYChart.Data<Number, Number> data : seriesInvoices.getData()) {
                    data.getNode().setVisible(true);
                }
            }
        }
    };
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

        initNullEntriesTable();
        getMaxAndMinDates();
    }

    @Override
    public void addMyListeners() {
        EventManager.getInstance().addEventListener(cel);
        myLineChart.addEventHandler(MouseEvent.MOUSE_PRESSED,mousePressedOnLineChart);
        myLineChart.addEventHandler(MouseEvent.MOUSE_DRAGGED,onMouseDraggedOnLineChart);
        myLineChart.addEventHandler(ScrollEvent.SCROLL,scrollHandleOnLineChart);
        cbStore.getSelectionModel().selectedItemProperty().addListener(handleStoreChange);
        cbYear.getSelectionModel().selectedItemProperty().addListener(handleYearChange);
        btnToggle.addEventHandler(ActionEvent.ACTION,btnToggleHandle);

    }
    @Override
    public void removeListeners(HelloController hc) {
        EventManager.getInstance().removeEventListener(cel);
        cbStore.getSelectionModel().selectedItemProperty().removeListener(handleStoreChange);
        cbYear.getSelectionModel().selectedItemProperty().removeListener(handleYearChange);
        myLineChart.removeEventHandler(MouseEvent.MOUSE_PRESSED,mousePressedOnLineChart);
        myLineChart.removeEventHandler(MouseEvent.MOUSE_DRAGGED,onMouseDraggedOnLineChart);
        myLineChart.removeEventHandler(ScrollEvent.SCROLL,scrollHandleOnLineChart);
        btnToggle.removeEventHandler(ActionEvent.ACTION,btnToggleHandle);
    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T)this ;
    }

    @Override
    public void setState() {
    }

    @Override
    public void getPreviousState() {
    }

    /**
     * Mikroprovlimata gia ftiaksimo:
     *
     * 1. ui thing. otan erthoun ta dedomena apo tin vasi
     *      gia kapoio logo eno sto linechart fainontai ta series
     *      ta tables kai to comboboxes einai adeia, mexri na ginei
     *      hover apo pano tous
     *
     * 2. Gia na fainontai ta tooltips vazoume listeners sta point ton
     *      series. isos tha prepei na ta vgazoume otan allazoume selida
     */


    private void initCbs() {
        cbYear.setItems(obsYears);
        cbStore.setItems(obsStores);
    }
    public void initTableResults(){
        TableColumn<SaleSummary,Date> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData->{
            Date date = cellData.getValue().getDate();
            return new ReadOnlyObjectWrapper<>(date);
        });
        dateCol.setCellFactory(new Callback<TableColumn<SaleSummary, Date>, TableCell<SaleSummary, Date>>() {
            @Override
            public TableCell<SaleSummary, Date> call(TableColumn<SaleSummary, Date> column) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Date item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(ABUsualInvoice.format.format(item));
                        }
                    }
                };
            }
        });

        TableColumn<SaleSummary,Number> moneyCol = new TableColumn<>("Money");
        moneyCol.setCellValueFactory(new PropertyValueFactory<>("sum"));
        tableResults.getColumns().setAll(dateCol,moneyCol);
        tableResults.setItems(obsSaleSummaries);
        tableResults.getSortOrder().add(dateCol);
        tableResults.sort();
        obsSaleSummaries.addListener(new ListChangeListener<SaleSummary>() {
            @Override
            public void onChanged(Change<? extends SaleSummary> change) {
                Platform.runLater(()->{
                    tableResults.sort();
                });
            }
        });
    }
    private void initNullEntriesTable(){
        TableColumn<String,String> textCol = new TableColumn<>("text");
        textCol.setCellValueFactory(celData->{
            return new ReadOnlyStringWrapper(celData.getValue());
        });
        tableNullDates.setItems(obsTextNullDates);
        tableNullDates.getColumns().setAll(textCol);
    }
    public void initLineChart() {
        xAxis = (NumberAxis) myLineChart.getXAxis();
        yAxis = (NumberAxis) myLineChart.getYAxis();
        myLineChart.setTitle("Sale Summary Over Time");
        myLineChart.getXAxis().setLabel("Date");
        myLineChart.getYAxis().setLabel("Sale Summary Value");
        myLineChart.getXAxis().setAutoRanging(false);
        myLineChart.getYAxis().setAutoRanging(false);
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
        } else if(!posDates){
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
            cbYear.getSelectionModel().selectedItemProperty().removeListener(handleYearChange);
            obsYears.setAll(choices);
            cbYear.getSelectionModel().select(0);
            cbYear.getSelectionModel().selectedItemProperty().addListener(handleYearChange);
        });
    }
    public void setObsStores (){
        Platform.runLater(()->{
            cbStore.getSelectionModel().selectedItemProperty().removeListener(handleStoreChange);
            obsStores.setAll(StoreNames.stringValues());
            if(!obsStores.isEmpty()){
                cbStore.getSelectionModel().select(0);
            }
            cbStore.getSelectionModel().selectedItemProperty().addListener(handleStoreChange);
        });
    }
    private void makePosSalesVisible(){
        seriesPosSales.getNode().setVisible(true);
        for(XYChart.Data<Number,Number> data : seriesPosSales.getData()){
            data.getNode().setVisible(true);
        }
    }
    private void calculateInvoicesSeries(){
        int a =0;
        Date currentDate = new Date();
        Date previousDate = new Date();
        BigDecimal dateSum = new BigDecimal(0);

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
            }
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
                    LocalDate finalLastDate = lastDate;
                    Platform.runLater(()->{
                        obsTextNullDates.add("we are missing "+ (finalLastDate.datesUntil(sumiDate).toList().size()-1)
                                +" date at "+HelpingFunctions.convertDateToLocalDate(sumi.getDate()).minusDays(1));
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
        data.nodeProperty().addListener(new ChangeListener<Node>() {
            @Override
            public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newNode) {
                if (newNode != null) {
                    newNode.setOnMouseEntered(mouseEvent -> {
                        // Hide the last tooltip before showing a new one
                        if (lastTooltip.isShowing()) {
                            lastTooltip.hide();
                        }

                        Date date = ResultsView.this.numericToDate(data.getXValue().longValue());
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
            }
        });
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
    private void getMaxAndMinDates(){

        MyTask fetchDates = new MyTask(()->null);

        fetchDates.setTaskLogic(()->{
            CompletableFuture<Void> getMinPosDate = CompletableFuture.runAsync(()->{
                PosEntryDAO posEntryDAO = new PosEntryDAO();
                minPosDate = posEntryDAO.getMinimumDate();
                Platform.runLater(()->{
                    fetchDates.setMyDescription(fetchDates.getMyDescription()+"\ngot min pos");
                });

                //System.out.println("the min pos date: "+minPosDate);
            });

            CompletableFuture<Void> getMaxPosDate = CompletableFuture.runAsync(()->{
                PosEntryDAO posEntryDAO = new PosEntryDAO();
                maxPosDate = posEntryDAO.getMaximumDate();
                Platform.runLater(()->{
                    fetchDates.setMyDescription(fetchDates.getMyDescription()+"\ngot max pos");
                });
                //System.out.println("the max pos date: "+maxPosDate);
            });

            CompletableFuture<Void> getMinDocDate = CompletableFuture.runAsync(()->{
                DocumentDAO documentDAO = new DocumentDAO();
                minDocDate = documentDAO.getMinimumDate();
                Platform.runLater(()->{
                    fetchDates.setMyDescription(fetchDates.getMyDescription()+"\ngot min doc");
                });

            });

            CompletableFuture<Void> getMaxDocDate = CompletableFuture.runAsync(()->{
                DocumentDAO documentDAO = new DocumentDAO();
                maxDocDate = documentDAO.getMaximumDate();
                Platform.runLater(()->{
                    fetchDates.setMyDescription(fetchDates.getMyDescription()+"\ngot max doc");
                });
            });

            CompletableFuture<Void> gettingDates = CompletableFuture.allOf(getMinPosDate,getMaxPosDate,getMinDocDate,getMaxDocDate);

            gettingDates.thenRun(()->{
                if(minPosDate==null||maxPosDate==null||minDocDate==null||maxDocDate==null){
                    System.out.println("one of the dates is null");
                    return;
                }
                setObsYears();
                setObsStores();
                getTheDataFromDB();
            });
            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "getting dates",
                "getting min and max dates",
                fetchDates
        );
    }
    public void getTheDataFromDB(){
        System.out.println("getting the data from db started");
        MyTask loadDataTask = new MyTask(()-> null);

        loadDataTask.setTaskLogic(()->{

            CompletableFuture<Void> loadingPosEntries = CompletableFuture.runAsync(() -> {
                Platform.runLater(()->{
                    cbStore.setDisable(true);
                    cbYear.setDisable(true);
                });

                System.out.println("loading the poses for example");
                System.out.println("future thread "+Thread.currentThread().getName());

                PosEntryDAO posEntryDAO = new PosEntryDAO();
                int year = cbYear.getValue();
                String store = cbStore.getValue();

                List<PosEntry> posEntryList = posEntryDAO.getPosEntriesByYearAndStore(year,store);

                HashMap<Date, SaleSummary> salesMap = new HashMap<>();
                for(PosEntry pos : posEntryList){
                    salesMap.computeIfAbsent(pos.getDate(), k -> new SaleSummary(pos.getDate()));
                    salesMap.get(pos.getDate()).addValueToSum(pos.money);
                }
                Platform.runLater(() -> {
                    obsSaleSummaries.setAll(salesMap.values());
                    loadDataTask.setMyDescription(loadDataTask.getMyDescription()+"\nloaded pos");
                });
                // Task 1 code
            });

            CompletableFuture<Void> loadingDocuments = CompletableFuture.runAsync(() -> {
                Platform.runLater(()->{
                    cbStore.setDisable(true);
                    cbYear.setDisable(true);
                });

                System.out.println("loading the documents");
                System.out.println("future thread "+Thread.currentThread().getName());

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

                Platform.runLater(()->{
                    loadDataTask.setMyDescription(loadDataTask.getMyDescription()+"\nloaded docs");
                    EventManager.getInstance().fireEnding(event);
                });
                // Task 2 code
            });

            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(loadingPosEntries, loadingDocuments);

            combinedFuture.thenRun(() -> {
                System.out.println("everything ended "+Thread.currentThread().getName());
                Platform.runLater(()->{
                    cbStore.setDisable(false);
                    cbYear.setDisable(false);
                    calculatePosAndRunningAvgSeries();
                    calculateInvoicesSeries();
                    myLineChart.layout();
                });

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
                Platform.runLater(()->{
                    loadDataTask.setMyDescription(loadDataTask.getMyDescription()+"\ndata loaded");
                });

                System.out.println("future FINAL thread "+Thread.currentThread().getName());
                // Code to run after both tasks are complete
            });

            return null;
        });

        parentDelegate.listManager.addTaskToActiveList(
                "future test",
                "testing the future",
                loadDataTask
        );
    }
}
