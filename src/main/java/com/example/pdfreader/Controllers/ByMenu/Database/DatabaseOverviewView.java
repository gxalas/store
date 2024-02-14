package com.example.pdfreader.Controllers.ByMenu.Database;


import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.Example.CustomEvent;
import com.example.pdfreader.MyCustomEvents.Example.CustomEventListener;
import com.example.pdfreader.MyCustomEvents.Example.EventManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.hibernate.SessionFactory;

import java.util.Date;
import java.util.List;

public class DatabaseOverviewView extends ChildController {
    public Button btnTest;
    public Button btlLoad;
    public TableView<DBError> tableErrors;
    public ObservableList<DBError> obsErrors = FXCollections.observableArrayList();
    private DBErrorDAO dbErrorDAO;

    private CustomEventListener cel = new CustomEventListener() {
        @Override
        public void onEnding(CustomEvent event) {
            System.out.println("from the results view \n the message: "+event.getMessage());
        }
    };

    @Override
    public void initialize(HelloController hc) {
        this.parentDelegate = hc;
        initButtons();
        initTablesErrors();
        initThreadDataLoading();
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
        return (T) this;
    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }

    private void initThreadDataLoading() {
        Thread loadDataThread = loadDataThread();
        loadDataThread.setDaemon(true);
        loadDataThread.start();
    }

    private Thread loadDataThread(){
        Task<Void> loadDataTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
                List<DBError> errors = dbErrorDAO.getAllErrors();
                Platform.runLater(()->{
                    obsErrors.setAll(errors);
                });
                return null;
            }
        };
        return new Thread(loadDataTask);
    }

    private void initTablesErrors() {
        TableColumn<DBError, Date> errorDateCol = new TableColumn<>();
        errorDateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<DBError,String> errorDescriptionCol = new TableColumn<>();
        errorDescriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<DBError, String> errorMessageCol = new TableColumn<>();
        errorMessageCol.setCellValueFactory(new PropertyValueFactory<>("errorMessage"));

        TableColumn<DBError,Long> errorIdCol = new TableColumn<>();
        errorIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<DBError, Void> errorDeleteColumn = getDbErrorVoidTableColumn();

        tableErrors.getColumns().setAll(errorIdCol,errorDateCol,errorDeleteColumn,errorDescriptionCol,errorMessageCol);
        tableErrors.setItems(obsErrors);
    }

    private void initButtons() {
        btnTest.setOnAction(actionEvent -> {
            /*
            DocumentDAO ddao = new DocumentDAO(dbErrorDAO);
            List<Document> docs = ddao.getAllDocuments();
            System.out.println("the documents are "+docs.size());
            for(Document doc :docs){
                System.out.println(doc.getFilePath()+" "+doc.getDocumentId()+" "+doc.getEntries().size());
            }
             */


        });


        btlLoad.setOnAction(actionEvent -> {
            /*
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    System.out.println("attempting to load pos entries");
                    PosEntryDAO posEntryDAO = new PosEntryDAO();
                    long start = System.nanoTime();
                    List<PosEntry> posEntries = posEntryDAO.getAllPosEntries();
                    long end = System.nanoTime();
                    System.out.println("it took "+(end-start)/1_000_000.00+" to load "+posEntries.size()+" pos entries");
                    return null;
                }
            };

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
             */
            parentDelegate.listManager.getActiveTasksList().
                    get(parentDelegate.listManager.getActiveTasksList().
                            size()-1).setAsRunning();

        });
    }

    private TableColumn<DBError, Void> getDbErrorVoidTableColumn() {
        TableColumn<DBError, Void> errorDeleteColumn = new TableColumn<>("Delete");
        Callback<TableColumn<DBError, Void>, TableCell<DBError, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<DBError, Void> call(final TableColumn<DBError, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Delete");
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            DBError data = getTableView().getItems().get(getIndex());
                            dbErrorDAO.deleteError(data.getId()); // Assuming DBError has an getId() method
                            obsErrors.remove(data);
                            //loadErrorsDB();
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                            setAlignment(Pos.CENTER);
                        }
                    }
                };
            }
        };
        errorDeleteColumn.setCellFactory(cellFactory);
        return errorDeleteColumn;
    }






}
