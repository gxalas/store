package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.DBError;
import com.example.pdfreader.DAOs.DBErrorDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.HibernateUtil;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ObservableQueue;
import com.example.pdfreader.MyCustomEvents.DBError.DBErrorEvent;
import com.example.pdfreader.MyCustomEvents.DBError.DBErrorListener;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedListener;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderListener;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.util.Date;
import java.util.List;

public class InvoicesImportView extends ChildController{
    @FXML
    public Text txtNumAtFolder,txtNumImported,txtNumFailed,txtNumToImport;
    @FXML
    public Button btnLoadFolder;
    @FXML
    public ProgressBar proBarFolderLoad = new ProgressBar();
    @FXML
    public ProgressBar proBarFileLoad = new ProgressBar();
    @FXML
    public TableView<Document> tableDocumentsImported;
    @FXML
    public TableView<Document> tableFailed;

    private final ObservableQueue.QueueListener<Document>  toImportQueueChanged = new ObservableQueue.QueueListener<Document>() {
        @Override
        public void elementAdded(Document element) {
            Platform.runLater(()->{
                txtNumToImport.setText("files to import : "+InvoicesImportView.super.parentDelegate.listManager.getToImportQueue().size());
            });
        }

        @Override
        public void elementRemoved(Document element) {
            Platform.runLater(()->{
                txtNumToImport.setText("files to import : "+InvoicesImportView.super.parentDelegate.listManager.getToImportQueue().size());
            });
        }

    };
    private final ChangeListener<Number> filesInFolderChanged = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
            txtNumAtFolder.setText("files at folder: "+t1);
        }
    };
    private final ListChangeListener<Document> importedListListener = new ListChangeListener<Document>() {
        @Override
        public void onChanged(Change<? extends Document> change) {
            Platform.runLater(()->{
                txtNumImported.setText("invoices imported: "+InvoicesImportView.super.parentDelegate.listManager.getImported().size());
                tableDocumentsImported.setItems(InvoicesImportView.super.parentDelegate.listManager.getImported());
            });
        }
    };
    private final ListChangeListener<Document> failedListListener = new ListChangeListener<Document>() {
        @Override
        public void onChanged(Change<? extends Document> change) {
            Platform.runLater(()->{
                txtNumFailed.setText(""+InvoicesImportView.super.parentDelegate.listManager.getFailed().size());
            });

        }
    };
    public final TracingFolderListener tracingFolderListener = new TracingFolderListener() {
        @Override
        public void tracingFolderStarts(TracingFolderEvent evt) {
            //listManager.getTraced().clear();
            btnLoadFolder.setVisible(false);
            System.out.println(" the tracing folder started ");
        }

        @Override
        public void tracingFolderEnds(TracingFolderEvent evt) {
            if(!InvoicesImportView.super.parentDelegate.listManager.getToImportQueue().isEmpty()) {
                btnLoadFolder.setVisible(true);
            }
            // - - - - - - - - - - - - - - - nice line that i don't know how it still works
            //int sum= (int) listManager.getTraced().stream().filter(doc -> !doc.duplicate).count();


        }
    };
    private final DocumentsImportedListener dil = new DocumentsImportedListener() {
        @Override
        public void documentsImported(DocumentsImportedEvent evt) {
            System.out.println("the documents are imported");
        }
    };
    private final DBErrorListener dbeListener = new DBErrorListener() {
        @Override
        public void errorLogged(DBErrorEvent event) {
            System.out.println("The listener heard an error at the database");
        }
    };
    @FXML
    public void initialize(){
    }
    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;

        initLoadFolderButton();
        initDocumentsImportedTable();
        initFailedTable();
        initTxts();
        initProBarFileLoad();

        System.out.println("the documents imported at the list manager are: "+super.parentDelegate.listManager.getImported().size());
    }
    public void initTxts(){
        txtNumToImport.setText("files to import : "+super.parentDelegate.listManager.getToImportQueue().size());
        txtNumFailed.setText(""+super.parentDelegate.listManager.getFailed().size());
        txtNumImported.setText("invoices imported "+super.parentDelegate.listManager.getImported().size());
        txtNumAtFolder.setText("files at folder: "+parentDelegate.numFilesInFolder.get());
    }




    @Override
    public void addMyListeners() {
        super.parentDelegate.actImportInvoices.setDisable(true);

        super.parentDelegate.addTracingFolderListeners(tracingFolderListener);
        super.parentDelegate.addDocumentProcessedListener(dil);


        //parentDelegate.addTracingFolderListeners(tracingFolderListener);
        super.parentDelegate.listManager.getImported().addListener(importedListListener);
        super.parentDelegate.listManager.getFailed().addListener(failedListListener);
        //listManager.getTraced().addListener(tracedListListener);

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        super.parentDelegate.listManager.getToImportQueue().addListener(toImportQueueChanged);
        super.parentDelegate.numFilesInFolder.addListener(filesInFolderChanged);
    }
    @Override
    public void removeListeners(HelloController hc) {
        hc.actImportInvoices.setDisable(false);

        super.parentDelegate.removeTracingFolderListeners(tracingFolderListener);
        super.parentDelegate.removeDocumentProcessedListener(dil);

        //parentDelegate.removeTracingFolderListeners(tracingFolderListener);
        super.parentDelegate.listManager.getImported().removeListener(importedListListener);
        super.parentDelegate.listManager.getFailed().removeListener(failedListListener);
        //listManager.getTraced().removeListener(tracedListListener);

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        super.parentDelegate.listManager.getToImportQueue().removeListener(toImportQueueChanged);
        hc.numFilesInFolder.removeListener(filesInFolderChanged);
    }
    @Override
    public <T extends ChildController> T getControllerObject(){
        return (T)this;
    }
    public void initLoadFolderButton(){
        btnLoadFolder.setOnAction(actionEvent -> {
            btnLoadFolder.setVisible(false);
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() {
                    processFiles();
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable exception = task.getException();
                if (exception != null) {
                    System.err.println("Background task failed:");
                    exception.printStackTrace();
                }
            });
            // Start the background task
            Thread thread = new Thread(task);
            thread.setDaemon(true); // Make the thread a daemon thread to avoid blocking the application from exiting
            thread.start();
        });
        btnLoadFolder.setVisible(!super.parentDelegate.listManager.getToImportQueue().isEmpty());
    }

    public void processFiles() {
        super.parentDelegate.listManager.fetchChecksums();
        super.parentDelegate.listManager.loadProductHashMap();
        //int size = listManager.getTraced().size();
        int size = super.parentDelegate.listManager.getToImportQueue().size();

        while (!super.parentDelegate.listManager.getToImportQueue().isEmpty()){
            updateProgressBarFolderLoading(size-super.parentDelegate.listManager.getToImportQueue().size()+1,size);
            Document newDoc = super.parentDelegate.listManager.getToImportQueue().poll();
            TextExtractions.process(newDoc,parentDelegate);
            if(newDoc.getDocumentId().compareTo("9033568261")==0){
                newDoc.addToErrorList("this is the one with the error");
            }
        }

        DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
        DocumentDAO ddao = new DocumentDAO(dbErrorDAO);
        List<DBError> errors = ddao.saveDocuments(super.parentDelegate.listManager.getImported());

        // Log all errors in bulk
        dbErrorDAO.saveDBErrors(errors);

        System.out.println("the imported are "+super.parentDelegate.listManager.getImported().size());
        super.parentDelegate.listManager.getImported().clear();
        super.parentDelegate.listManager.getProductHashMap().clear();
        super.parentDelegate.listManager.getChecksums().clear();

        super.parentDelegate.fireDocumentProcessedEvent(new DocumentsImportedEvent(this));
    }
    public void updateProgressBarFolderLoading(int count, int totalFiles) {
        double progress = (double) count / totalFiles;
        Platform.runLater(() -> {
            proBarFolderLoad.setProgress(progress);
        });
    }

    private void initDocumentsImportedTable() {
        TableColumn<Document, String> docIdCol = new TableColumn<>("Doc Id");
        docIdCol.setCellValueFactory(new PropertyValueFactory<>("documentId"));
        TableColumn<Document,String> docNameCol = new TableColumn<>("File name");
        docNameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        TableColumn<Document,String> docPathCol = new TableColumn<>("File path");
        docPathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        TableColumn<Document,String> docChecksumCol = new TableColumn<>("Checksum");
        docChecksumCol.setCellValueFactory(new PropertyValueFactory<>("checksum"));
        TableColumn<Document,String> docTypeCol = new TableColumn<>("Type");
        docTypeCol.setCellValueFactory(new PropertyValueFactory<>("Type"));
        TableColumn<Document, Date> docDateImportedCol = new TableColumn<>("Imported Date");
        docDateImportedCol.setCellValueFactory(new PropertyValueFactory<>("ImportDate"));

        tableDocumentsImported.setItems(super.parentDelegate.listManager.getImported());
        tableDocumentsImported.getColumns().setAll(docNameCol,docPathCol,docIdCol,docChecksumCol,
                docTypeCol,docDateImportedCol);
    }
    private void initFailedTable() {
        // Column for the description
        TableColumn<Document, String> descriptionCol = new TableColumn<>("path");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("path"));

        tableFailed.getColumns().setAll(descriptionCol);
        tableFailed.setItems(super.parentDelegate.listManager.getFailed());

        // Columns for the ArrayList values
        for (int i = 0; i < 5; i++) {
            final int index = i;
            TableColumn<Document, String> valueCol = new TableColumn<>("Error " + (i + 1));
            valueCol.setCellValueFactory(cellData -> {
                Document doc = cellData.getValue();
                if (doc.getErrorList().size() > index) {
                    return new SimpleStringProperty(doc.getErrorList().get(index));
                } else {
                    return new SimpleStringProperty(""); // or null if you prefer
                }
            });
            tableFailed.getColumns().add(valueCol);
        }
    }


    public void initProBarFileLoad() {
        parentDelegate.percentOfTracing.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                Platform.runLater(()->{
                    proBarFileLoad.setProgress(t1.doubleValue());
                });

            }
        });

    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }

}
