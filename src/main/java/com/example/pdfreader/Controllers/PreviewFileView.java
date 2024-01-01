package com.example.pdfreader.Controllers;

import com.example.pdfreader.Controllers.States.PreviewFileViewState;
import com.example.pdfreader.DAOs.DBErrorDAO;
import com.example.pdfreader.DAOs.DocEntryDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.HibernateUtil;
import com.example.pdfreader.DTOs.DocEntryDTO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedListener;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.example.pdfreader.DAOs.HibernateUtil.*;

public class PreviewFileView extends ChildController {
    private final ObservableList<Document> obsMerged = FXCollections.observableArrayList(new ArrayList<>());
    private final ObservableList<DocEntryDTO> obsDocEntries = FXCollections.observableArrayList(new ArrayList<>()); //oi eggrafes enos eggrafou
    @FXML
    public TableView<DocEntryDTO> viewDocEntries;
    @FXML
    public TableView<Document> viewDocTable;
    @FXML
    public ComboBox<String> cbFilterPreview;
    @FXML
    public ScrollPane pdfScrollPane;
    @FXML
    public Button btnJump;
    @FXML
    public Text docSum;
    @FXML
    public Text errorTxt;
    public Text txtNumTableEntries;
    private DocumentDAO ddao;
    private DocEntryDAO dedao;
    private MyTask loadDocuments;

    private List<Document> dbDocuments= new ArrayList<Document>();

    private final DocumentsImportedListener myListener = new DocumentsImportedListener() {
        @Override
        public void documentsImported(DocumentsImportedEvent evt) {
            System.out.println("Preview got notified that documents were imported");
            if(cbFilterPreview.getValue().compareTo("Failed")==0){
                obsMerged.setAll(PreviewFileView.super.parentDelegate.listManager.getFailed());
            } else if(cbFilterPreview.getValue().compareTo("Imported")==0){
                obsMerged.setAll(ddao.getAllDocuments());
            } else if(cbFilterPreview.getValue().compareTo("All")==0){
                obsMerged.setAll(PreviewFileView.super.parentDelegate.listManager.getImported());
                obsMerged.addAll(PreviewFileView.super.parentDelegate.listManager.getFailed());
            }
        }
    };
    public SplitPane innerContainer;


    @FXML
    public void initialize(){
        obsMerged.addListener(new ListChangeListener<Document>() {
            @Override
            public void onChanged(Change<? extends Document> change) {
                txtNumTableEntries.setText(""+obsMerged.size());
            }
        });








        //Thread thread = new Thread(loadDocuments);
        //thread.setDaemon(true);
        //thread.start();

        initCBFilterPreview();
        initViewDocTable();
        initViewDocEntries();
        initBtnJumpToFolder();
        AnchorPane.setTopAnchor(innerContainer,0.0);
        AnchorPane.setBottomAnchor(innerContainer,0.0);
    }

    private void initCalculateMerged() {
        if(cbFilterPreview.getValue().compareTo("Failed")==0){
            obsMerged.setAll(super.parentDelegate.listManager.getFailed());
        } else if(cbFilterPreview.getValue().compareTo("Imported")==0){
            obsMerged.setAll(super.parentDelegate.listManager.getImported());
        } else if(cbFilterPreview.getValue().compareTo("All")==0){
            obsMerged.setAll(super.parentDelegate.listManager.getImported());
            obsMerged.addAll(super.parentDelegate.listManager.getFailed());
        }
    }

    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        loadDocuments = new MyTask(()->{
            long startTime = System.nanoTime();
            DBErrorDAO dbErrorDAO = new DBErrorDAO(new ErrorEventManager());
            ddao = new DocumentDAO(dbErrorDAO);
            dedao = new DocEntryDAO();
            List<Document> tempDocs = ddao.getAllDocuments();
            try (Session session = HibernateUtil.getSessionFactory().openSession()){
                tempDocs.forEach(document -> Hibernate.initialize(document.getErrorList()));
            }
            Platform.runLater(()->{
                dbDocuments = tempDocs;
                viewDocTable.refresh();
            });

            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            System.out.println("Execution time: " + duration / 1_000_000.0 + " ms");

            return null;
        });
        //loadDocuments.setMyTitle("loading Documents");
        //loadDocuments.setMyDescription("loading of the documents");

        parentDelegate.listManager.addTaskToActiveList(
                "loading Documents",
                "load documents with DAO and DBerror",
                loadDocuments

        );

        //parentDelegate.listManager.getActiveTasksList().add(0,loadDocuments);
        cbFilterPreview.getSelectionModel().select(1);



        try {
            initPdfScrollPane();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initCalculateMerged();
    }

    @Override
    public void addMyListeners() {
        parentDelegate.actPreviewInvoices.setDisable(true);
        parentDelegate.addDocumentProcessedListener(myListener);
    }

    @Override
    public void removeListeners(HelloController hc) {
        hc.actPreviewInvoices.setDisable(false);
        hc.removeDocumentProcessedListener(myListener);
    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T)this;
    }



    private void initCBFilterPreview() {
        cbFilterPreview.getItems().add("Failed");
        cbFilterPreview.getItems().add("Duplicated");
        cbFilterPreview.getItems().add("Imported");
        cbFilterPreview.getItems().add("All");
        cbFilterPreview.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if(observableValue.getValue().compareTo("Failed")==0){
                    Platform.runLater(()->{
                        List<Document> filtered = PreviewFileView.super.parentDelegate.listManager.getFailed().stream()
                                .filter(document -> !document.getErrorList().contains("duplicate"))
                                .toList();
                        obsMerged.setAll(filtered);
                        //System.out.println("the failed are :"+listManager.getFailed().size());
                    });
                } else if (observableValue.getValue().compareTo("Duplicated")==0){
                    List<Document> filtered = PreviewFileView.super.parentDelegate.listManager.getFailed().stream()
                            .filter(document -> document.getErrorList().contains("duplicate"))
                            .toList();
                    Platform.runLater(()->{
                        obsMerged.setAll(filtered);
                        //System.out.println("the failed are :"+listManager.getFailed().size());
                    });
                }else if(observableValue.getValue().compareTo("Imported")==0){
                    Platform.runLater(()->{
                        obsMerged.setAll(dbDocuments);
                        //obsMerged.setAll(listManager.getImported());
                    });
                } else if(observableValue.getValue().compareTo("All")==0){
                    Platform.runLater(()->{
                        obsMerged.setAll(dbDocuments);
                        obsMerged.addAll(PreviewFileView.super.parentDelegate.listManager.getFailed());
                    });
                }
            }
        });
        //cbFilterPreview.getSelectionModel().select(2);
    }

    public void initViewDocTable(){
        TableColumn<Document,String> documentIdCol = new TableColumn<>("Document Id");
        documentIdCol.setCellValueFactory(new PropertyValueFactory<>("documentId"));

        TableColumn<Document,String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathCol.setPrefWidth(120);

        TableColumn<Document,String> duplicateCol = new TableColumn<>("is duplicate");
        duplicateCol.setCellValueFactory(new PropertyValueFactory<>("duplicate"));

        TableColumn<Document,String> docTypeCol = new TableColumn<>("Type");
        docTypeCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType().name()));

        TableColumn<Document, Date> docDateCol = new TableColumn<>("Date");
        docDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Document, String> docPromTypeCol = new TableColumn<>("Promitheutis");
        docPromTypeCol.setCellValueFactory(new PropertyValueFactory<>("promType"));

        TableColumn<Document,String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(cellData->{
            if (cellData.getValue().getSupplier()!=null){
                return new ReadOnlyObjectWrapper<>(cellData.getValue().getSupplier().getName());
            }
            return new ReadOnlyStringWrapper("no supplier yet");
        });

        TableColumn<Document, String> docStoreCol = new TableColumn<>("Store");
        docStoreCol.setCellValueFactory(cellData->{
            return new ReadOnlyStringWrapper(cellData.getValue().getStore().getName());
        });

        TableColumn<Document, BigDecimal> docSumCheckeCol = new TableColumn<>("sum Entries");
        docSumCheckeCol.setCellValueFactory(new PropertyValueFactory<>("sumEntries"));

        TableColumn<Document, String> docSumCol = new TableColumn<>("Sum Retrieved");
        docSumCol.setCellValueFactory(new PropertyValueFactory<>("sumRetrieved"));

        TableColumn<Document, String> docSumDiffCol = new TableColumn<>("Sum Difference");
        docSumDiffCol.setCellValueFactory(new PropertyValueFactory<>("sumDiff"));

        TableColumn<Document,String> docHasError = new TableColumn<>("has Error");
        docHasError.setCellValueFactory(new PropertyValueFactory<>("hasError"));

        docDateCol.setCellFactory(column -> new TableCell<Document, Date>() {
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

        viewDocTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        viewDocTable.setItems(obsMerged);
        //viewDocTable.getItems().addAll(listManager.getImported());
        viewDocTable.getColumns().setAll(docStoreCol,documentIdCol,pathCol,docDateCol,
                docPromTypeCol,supplierCol,duplicateCol,docTypeCol,docSumCol,docSumCheckeCol,
                docSumDiffCol, docHasError);

        viewDocTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        if(!newValue.getErrorList().isEmpty()){
                            System.out.println("the error "+newValue.getErrorList().get(0));
                        }
                        handleRowClick(newValue);
                    }
                }
        );



        viewDocTable.setRowFactory(tv -> new TableRow<Document>() {
            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.getErrorList().isEmpty()) {
                    setStyle("-fx-background-color: #FFCCCC;");
                } else {
                    setStyle("");
                }
            }
        });



    }
    private void handleRowClick(Document item){
        // Implement your logic here
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if(!item.getEntries().isEmpty()){
                    Task<Void> loadItems = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            long start = System.nanoTime();
                            List<DocEntryDTO> loadedEntries = dedao.getDocEntriesByDocument(item);
                            long end = System.nanoTime();
                            System.out.println("execution time is : "+(end-start)/1_000_000.00);
                            obsDocEntries.setAll(loadedEntries);
                            return null;
                        }
                    };
                    Thread thread = new Thread(loadItems);
                    thread.setDaemon(true);
                    thread.start();
                } else {
                    obsDocEntries.clear();
                }
                if(PreviewFileView.super.parentDelegate.listManager.getImported().contains(item)){
                    Platform.runLater(()->{
                        docSum.setText(item.getSumRetrieved().toString());
                    });
                } else {
                    obsDocEntries.clear();
                    Platform.runLater(()->{
                        docSum.setText("this item is NOT imported");
                    });
                }
                Platform.runLater(()->{
                    try {
                        loadPdfViewerPane(item);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
            }

            @Override
            protected void failed() {
                super.failed();
                // Handle any exceptions here
                getException().printStackTrace();
            }
        };
        new Thread(loadTask).start();
    }
    private void loadPdfViewerPane(Document doc) throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                VBox container = new VBox(5);
                container.setAlignment(Pos.CENTER);
                if(!Files.exists(Paths.get(doc.getPath()))){
                    System.out.println("couldn't find the file to load id");
                    container.getChildren().add(new Text("didn't find pdf"));
                    Platform.runLater(() -> {
                        pdfScrollPane.setFitToWidth(true);
                        pdfScrollPane.setContent(container);
                    });
                    return null;
                }
                PDDocument document = PDDocument.load(new File(doc.getFilePath()));
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                double initialZoomLevel = 0.4;
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(image.getWidth() * initialZoomLevel);
                    imageView.setFitHeight(image.getHeight() * initialZoomLevel);
                    imageView.setPreserveRatio(true);
                    container.getChildren().add(imageView);
                }
                Platform.runLater(() -> {
                    pdfScrollPane.setFitToWidth(true);
                    pdfScrollPane.setContent(container);
                });
                document.close();
                return null;
            }
        };
        task.run();
    }
    private void initViewDocEntries(){
        TableColumn<DocEntryDTO, String> indexColumn = new TableColumn<>("#");
        indexColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(Integer.toString(getIndex() + 1)); // +1 because index is 0-based
                }
            }
        });
        indexColumn.setPrefWidth(40);

        TableColumn<DocEntryDTO,String> productCodeCol = new TableColumn<>("Product Code");
        productCodeCol.setCellValueFactory(cellData->{
            DocEntry entry = cellData.getValue().getDocEntry();
            Product product = cellData.getValue().getDocEntry().getProduct();
            return new ReadOnlyStringWrapper(product.getCode());
        });
        //productIdCol.setCellValueFactory(new PropertyValueFactory<>("productCode"));

        TableColumn<DocEntryDTO,String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue().getDocEntry().getProduct();
            return new ReadOnlyStringWrapper(product.getDescription());
        });
        //descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<DocEntryDTO,String> masterCol = new TableColumn<>("Master");
        masterCol.setCellValueFactory(cellData->{
            String master = cellData.getValue().getDocEntry().getProductMaster();
            return new ReadOnlyStringWrapper(master);
        });
        //masterCol.setCellValueFactory(new PropertyValueFactory<>("productMaster"));

        TableColumn<DocEntryDTO,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            String department = "";
            try {
                department = cellData.getValue().getDocEntry().getProduct().getStoreBasedAttributes().get(0).getDepartment();
            } catch (Exception e){

            }
            return new ReadOnlyStringWrapper(department);
        });
        //eanCol.setCellValueFactory(new PropertyValueFactory<>("lastEan"));

        TableColumn<DocEntryDTO,String> boxesCol = new TableColumn<>("Boxes");
        boxesCol.setCellValueFactory(cellData->{
            String boxes = cellData.getValue().getDocEntry().getBoxes().toString();
            return new ReadOnlyStringWrapper(boxes);
        });


        //boxesCol.setCellValueFactory(new PropertyValueFactory<>("boxes"));

        TableColumn<DocEntryDTO,String> perBoxCol = new TableColumn<>("Units per Box");
        perBoxCol.setCellValueFactory(cellData->{
            String perBoxes = cellData.getValue().getDocEntry().getUnitsPerBox().toString();
            return new ReadOnlyStringWrapper(perBoxes);
        });


        //perBoxCol.setCellValueFactory(new PropertyValueFactory<>("unitsPerBox"));

        TableColumn<DocEntryDTO,String> unitsCol = new TableColumn<>("Units");
        unitsCol.setCellValueFactory(cellData->{
            String units = cellData.getValue().getDocEntry().getUnits().toString();
            return new ReadOnlyStringWrapper(units);
        });

        //unitsCol.setCellValueFactory(new PropertyValueFactory<>("units"));

        TableColumn<DocEntryDTO,String> unitPriceCol = new TableColumn<>("Price");

        unitPriceCol.setCellValueFactory(cellData->{
            String price = cellData.getValue().getDocEntry().getUnitPrice().toString();
            return new ReadOnlyStringWrapper(price);
        });

        //unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<DocEntryDTO,String> totalPriceCol = new TableColumn<>("Total Price");

        totalPriceCol.setCellValueFactory(cellData->{
            String totalP = cellData.getValue().getDocEntry().getTotalPrice().toString();
            return new ReadOnlyStringWrapper(totalP);
        });


        //totalPriceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        TableColumn<DocEntryDTO,String> netValCol = new TableColumn<>("Net Value");

        netValCol.setCellValueFactory(cellData->{
            String netValue = cellData.getValue().getDocEntry().getNetValue().toString();
            return new ReadOnlyStringWrapper(netValue);
        });
        //netValCol.setCellValueFactory(new PropertyValueFactory<>("netValue"));

        TableColumn<DocEntryDTO,String> vatValCol = new TableColumn<>("Vat Value");
        vatValCol.setCellValueFactory(cellData->{
            String vatValue = cellData.getValue().getDocEntry().getVatValue().toString();
            return new ReadOnlyStringWrapper(vatValue);
        });


        //vatValCol.setCellValueFactory(new PropertyValueFactory<>("vatValue"));

        TableColumn<DocEntryDTO,String> vatPercentCol = new TableColumn<>("Vat Percent");
        vatPercentCol.setCellValueFactory(cellData->{
            String vatP = cellData.getValue().getDocEntry().getVatPercent().toString();
            return new ReadOnlyStringWrapper(vatP);
        });

        TableColumn<DocEntryDTO,String> suppliersCol = new TableColumn<>("Suppliers");
        suppliersCol.setCellValueFactory(cellData->{
            String supps = cellData.getValue().getSuppliers();
            return new ReadOnlyStringWrapper(supps);
        });

        //vatPercentCol.setCellValueFactory(new PropertyValueFactory<>("vatPercent"));

        Platform.runLater(()->{
            viewDocEntries.setItems(obsDocEntries);
            viewDocEntries.getColumns().setAll(indexColumn,productCodeCol,descriptionCol,suppliersCol,masterCol,departmentCol,boxesCol,perBoxCol,unitsCol,unitPriceCol
                    ,totalPriceCol,netValCol,vatValCol,vatPercentCol);

            viewDocEntries.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(DocEntryDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                    } else if (!item.getDocEntry().getErrorLogs().isEmpty()) {
                        // Change background color or apply any other styling you need
                        setStyle("-fx-background-color: #FFCCCC;");
                    } else {
                        setStyle("");
                    }
                }
            });
        });
        viewDocEntries.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DocEntryDTO>() {
            @Override
            public void changed(ObservableValue<? extends DocEntryDTO> observableValue, DocEntryDTO docEntry, DocEntryDTO t1) {
                if(t1!=null){
                    if(t1.getDocEntry().getErrorLogs()!=null && !t1.getDocEntry().getErrorLogs().isEmpty()){
                        errorTxt.setText(t1.getDocEntry().getErrorLogs().get(0));
                        return;
                    }
                }

                errorTxt.setText("no errors found in line");
            }
        });
    }
    private void initBtnJumpToFolder() {
        btnJump.setOnAction(actionEvent -> {
            if (viewDocTable.getSelectionModel().getSelectedItem()!=null){
                File selectedFile = new File (viewDocTable.getSelectionModel().getSelectedItem().getFilePath());

                if (selectedFile != null) {
                    try {
                        String os = System.getProperty("os.name", "").toLowerCase();

                        if (os.contains("mac")) {
                            Runtime.getRuntime().exec(new String[]{"open", "-R", selectedFile.getAbsolutePath()});
                        } else if (os.contains("win")) {
                            ProcessBuilder pb = new ProcessBuilder(
                                    "explorer.exe",
                                    "/select," + selectedFile.getAbsolutePath()
                            );
                            pb.start();
                        } else {
                            System.out.println("Unsupported OS for opening file explorer: " + os);
                            // Optionally, open the file's parent directory without selecting the file on unsupported OS.
                            // Desktop.getDesktop().open(selectedFile.getParentFile());
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to open file explorer for: " + selectedFile.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private void initPdfScrollPane() throws IOException {
        if(!super.parentDelegate.listManager.getToImportQueue().isEmpty()){
            loadPdfViewerPane(super.parentDelegate.listManager.getToImportQueue().toList().get(0));
        }
    }
    @Override
    public void setState() {
        System.out.println("setting the state of preview");
        TableColumn column = null;
        Document selected = null;
        if(!viewDocTable.getSortOrder().isEmpty()){
            column =  viewDocTable.getSortOrder().get(0);
        }
        if(viewDocTable.getSelectionModel().getSelectedItem()!=null){
            selected = viewDocTable.getSelectionModel().getSelectedItem();
        }
        parentDelegate.previewFileViewState = new PreviewFileViewState(cbFilterPreview.getSelectionModel().getSelectedIndex(),column,selected);

    }

    @Override
    public void getPreviousState() {
        if(parentDelegate.previewFileViewState!=null){
            loadDocuments.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent workerStateEvent) {
                    cbFilterPreview.getSelectionModel().select(parentDelegate.previewFileViewState.type());
                    if(parentDelegate.previewFileViewState.tableColumn()!=null){
                        Platform.runLater(()->{
                            viewDocTable.getSortOrder().setAll(parentDelegate.previewFileViewState.tableColumn());
                            viewDocTable.sort();
                        });
                    }
                    if (parentDelegate.previewFileViewState.selected()!=null){
                        Platform.runLater(()->{
                            System.out.println("trying to select for the state");
                            viewDocTable.scrollTo(viewDocTable.getItems().indexOf(parentDelegate.previewFileViewState.selected()));
                            viewDocTable.getSelectionModel().select(parentDelegate.previewFileViewState.selected());
                        });
                    }

                }
            });

        } else {
            System.out.println("no previous state for preview file");
        }
    }
}
