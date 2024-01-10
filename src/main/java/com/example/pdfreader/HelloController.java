package com.example.pdfreader;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.Controllers.States.FilterInvoicesState;
import com.example.pdfreader.Controllers.States.PreviewFileViewState;
import com.example.pdfreader.Controllers.States.ProductViewState;
import com.example.pdfreader.Controllers.States.SuppliersConfigState;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.MyTaskState;
import com.example.pdfreader.Helpers.MyThreads;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedEvent;
import com.example.pdfreader.MyCustomEvents.DocumentsImportedListener;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.MyCustomEvents.TracingFolderListener;
import com.example.pdfreader.Sinartiseis.Serialization;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.SySettings;
import jakarta.persistence.EntityManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import javafx.util.Duration;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

import javax.swing.event.EventListenerList;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloController {
    public IntegerProperty numFilesInFolder = new SimpleIntegerProperty(0);
    public DoubleProperty percentOfTracing = new SimpleDoubleProperty(0.0);
    public ListManager listManager;
    @FXML
    public MenuItem actSaveFile;
    @FXML
    public MenuItem actMemoryStatus;
    @FXML
    public MenuItem actGC;
    @FXML
    public MenuItem actSelectFolder;
    @FXML
    public MenuItem actImportInvoices;
    @FXML
    public MenuItem actStatus;
    @FXML
    public MenuItem actPreviewInvoices;
    @FXML
    public MenuItem actProductsPage;
    @FXML
    public MenuItem actDbOverview;
    @FXML
    public MenuItem actImportTxts;
    public MenuItem actResultsOverview;
    public MenuItem actSuppConfig;
    public MenuItem actFilterInvoices;
    public MenuItem actProductSuppliers;
    public MenuItem actSuppOverview;
    public DirectoryChooser directoryChooser = new DirectoryChooser();
    @FXML
    public AnchorPane parentContainer;
    public ObservableList<String> txtAMemoryStatusList = FXCollections.observableArrayList();
    public ChildController currentChild;



    private ExecutorService executorService = Executors.newCachedThreadPool();
    private final Task<Void> watcher = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            addWatcherToInvoicesFolder();
            return null;
        }
    };
    private final Thread watcherThread = new Thread(watcher);
    @FXML
    public HBox infoStrip;
    public Timeline timeline = new Timeline();
    public Text txtInfoMessage;
    public final Queue<String> messageQueue = new LinkedList<>();
    public ListView<MyTask> listActiveTasks;
    public ListView<MyThreads> listFinishedTasks;
    private Timeline notificationTimeline;
    public FilterInvoicesState filterInvoicesState;
    public PreviewFileViewState previewFileViewState;
    public ProductViewState productViewState;
    public SuppliersConfigState suppliersConfigState;


    @FXML
    public void initialize(){
        System.out.println("The initialization of the HelloController has happened");
        initMenuItems();
        watcherThread.setDaemon(true);
        watcherThread.start();
        ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.LIGHTBLUE);

        // Bind the background property of the pane to the color property
        infoStrip.backgroundProperty().
                bind(Bindings.createObjectBinding(() ->
                        new Background(new BackgroundFill(color.get(), null, null)),
                        color));

        KeyValue keyValue1 = new KeyValue(color, Color.LIGHTBLUE);
        KeyValue keyValue2 = new KeyValue(color, Color.LIGHTGREEN);
        KeyFrame keyFrame1 = new KeyFrame(Duration.seconds(0), keyValue1);
        KeyFrame keyFrame2 = new KeyFrame(Duration.seconds(1), keyValue2);
        KeyFrame keyFrame3 = new KeyFrame(Duration.seconds(2), keyValue1);
        timeline.getKeyFrames().addAll(keyFrame1, keyFrame2, keyFrame3);
        timeline.setAutoReverse(true);
        timeline.setCycleCount(3);

        //timeline.stop();

        // Timeline to manage the display and wait logic
        notificationTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> displayNextMessage()));
        notificationTimeline.setCycleCount(Timeline.INDEFINITE);


    }
    public void initActiveTaskListView() {
        System.out.println(" * * * * * Initializing the task list view * * * * * *");
        listActiveTasks.setCellFactory(listView -> new ListCell<MyTask>() {
            private ChangeListener<MyTaskState> stateChangeListener;

            @Override
            protected void updateItem(MyTask item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setMinHeight(60);
                    Label titleLabel = new Label();
                    Label descriptionLabel = new Label();
                    Label stateLabel = new Label();



                    titleLabel.textProperty().bind(item.getTitleProperty());
                    descriptionLabel.textProperty().bind(item.getMyDescriptionProperty());


                    stateLabel.textProperty().bind(Bindings.createStringBinding(() ->
                            item.getMyStateProperty().get().toString(), item.getMyStateProperty()));

                    VBox vbox = new VBox(titleLabel, descriptionLabel, stateLabel);
                    vbox.setSpacing(1); // Adjust as needed
                    //vbox.setFillWidth(true); // Ensures children fill the width of the VBox
                    vbox.setMinHeight(USE_PREF_SIZE);
                    setGraphic(vbox);

                    titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    descriptionLabel.setFont(Font.font("System", FontPosture.ITALIC, 12));

                    // Remove existing listener
                    if (stateChangeListener != null) {
                        item.getMyStateProperty().removeListener(stateChangeListener);
                    }




                    // Add new listener
                    stateChangeListener = (obs, oldState, newState) -> Platform.runLater(() -> {
                        System.out.println("Applying update for an item: { '" + item.getMyTitle() + "' } new state : " + newState+" this: "+getIndex());
                        applyStyleBasedOnState(newState, this);
                        refreshCell();
                        //listActiveTasks.refresh();
                    });
                    item.getMyStateProperty().addListener(new WeakChangeListener<>(stateChangeListener));

                    applyStyleBasedOnState(item.getMyState(), this);
                }
            }
            public void refreshCell() {
                updateItem(getItem(), false);
            }
        });

        listManager.getActiveTasksList().addListener(new ListChangeListener<MyTask>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends MyTask> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        List<? extends MyTask> newTasks = change.getAddedSubList();
                        for (MyTask task : newTasks) {
                            System.out.println("\n" +
                                    "the state is "+task.getMyState()+" \n");
                            task.setMyState(MyTaskState.RUNNING);
                            executorService.submit(task);
                            };


                            //Thread t = new Thread(task);
                            //t.setDaemon(true);
                            //task.setMyState(MyTaskState.RUNNING);
                            //t.start();

                            //thread.setAsRunning();

                            // For example, start the thread
                            //thread.start();
                        }
                    }
                }

        });
        listActiveTasks.setItems(listManager.getActiveTasksList());
    }
    private void applyStyleBasedOnState(MyTaskState state, ListCell<MyTask> cell) {
        if (state != null) {
            Platform.runLater(() -> {
                switch (state) {
                    case PENDING:
                        //System.out.println("Applying style for state: " + state+" yellow"); // Debugging statement
                        cell.setStyle("-fx-background-color: yellow;");
                        break;
                    case RUNNING:
                        //System.out.println("Applying style for state: " + state+" orange"); // Debugging statement
                        cell.setStyle("-fx-background-color: orange;");
                        break;
                    case COMPLETED:
                        //System.out.println("Applying style for state: " + state+" green"); // Debugging statement
                        cell.setStyle("-fx-background-color: lightgreen;");
                        break;
                    case FAILED:
                        //System.out.println("Applying style for state: " + state+" coral"); // Debugging statement
                        cell.setStyle("-fx-background-color: lightcoral;");
                        break;
                    default:
                        //System.out.println("Applying style for state: " + state+" default"); // Debugging statement
                        cell.setStyle("");
                        break;
                }
            });
        } else {
            cell.setStyle("");
        }
    }



    public void setListManager(ListManager manager) {
        this.listManager = manager;
        initActiveTaskListView();

    }





    protected EventListenerList docsImportedListeners = new EventListenerList();
    public void addDocumentProcessedListener(DocumentsImportedListener listener) {
        docsImportedListeners.add(DocumentsImportedListener.class, listener);
    }
    public void removeDocumentProcessedListener(DocumentsImportedListener listener) {
        docsImportedListeners.remove(DocumentsImportedListener.class, listener);
    }
    public void fireDocumentProcessedEvent(DocumentsImportedEvent evt) {
        Object[] listeners = docsImportedListeners.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == DocumentsImportedListener.class) {
                ((DocumentsImportedListener) listeners[i+1]).documentsImported(evt);
            }
        }
        enqueueMessage("importing of documents finished");
        System.out.println("documents imported");
    }





    protected EventListenerList tracingFolderListeners = new EventListenerList();
    public void addTracingFolderListeners (TracingFolderListener tfListener){
        tracingFolderListeners.add(TracingFolderListener.class,tfListener);
    }
    public void removeTracingFolderListeners(TracingFolderListener tfListener){
        tracingFolderListeners.remove(TracingFolderListener.class, tfListener);
    }
    public void fireStartTracingFolder(TracingFolderEvent evt){
        Object[] listeners = tracingFolderListeners.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == TracingFolderListener.class) {
                ((TracingFolderListener) listeners[i+1]).tracingFolderStarts(evt);
            }
        }
    }
    public void fireEndTracingFolder(TracingFolderEvent evt){
        Object[] listeners = tracingFolderListeners.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == TracingFolderListener.class) {
                ((TracingFolderListener) listeners[i+1]).tracingFolderEnds(evt);
            }
        }
        Platform.runLater(()->{
            enqueueMessage("tracing has ended");
        });
    }

    private void initMenuItems() {
        actSaveFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                actSaveFile.setDisable(true);
                try {
                    Serialization.saveImported(getControllerObject());
                    txtAMemoryStatusList.add("^ ^ ^ ^ ^ File Saved ^ ^ ^ ^ ^ ^ ->"+Calendar.getInstance().getTime()+"\n");
                } catch (IOException e) {
                    txtAMemoryStatusList.add(" - - - - - - - error at saving the invoices - - - - - - - -\n");
                    enqueueMessage("Error at saving the Invoices");
                    throw new RuntimeException(e);
                }
                actSaveFile.setDisable(false);
                playSoundNotification();
                enqueueMessage("Invoices saved");
            }
        });

        actMemoryStatus.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
                MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();

                String status ="- - - - - - "+Calendar.getInstance().getTime()+"- - - - - - - - \n";
                status += "Memory Bean: "+ memoryBean+"\n";
                status +="Heap Memory Usage: " + heapMemoryUsage+"\n";
                status +="Non-Heap Memory Usage: " + nonHeapMemoryUsage+"\n";
                status +="- - - - - - - - - - - - - - - - - - - - - - - - \n";
                txtAMemoryStatusList.add(status);
                System.out.println(status);
            }
        });

        actGC.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                System.gc();
                txtAMemoryStatusList.add(" * * * * * * Garbage Collected * * * * * * * ->"+Calendar.getInstance().getTime()+"\n");
            }
        });

        actProductsPage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                loadController("products-view.fxml");
            }
        });
        directoryChooser.setTitle("Select a Folder");
        File defaultDirectory = new File(SySettings.PATH_TO_FOLDER.getPath());
        directoryChooser.setInitialDirectory(defaultDirectory);
        actSelectFolder.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                File selectedDirectory = directoryChooser.showDialog(actSelectFolder.getParentPopup().getOwnerWindow());
                // Do something with the selected directory
                if (selectedDirectory != null) {
                    System.out.println("Selected folder: " + selectedDirectory.getAbsolutePath());
                    SySettings.PATH_TO_FOLDER.setPath(selectedDirectory.getPath());
                    Serialization.saveSySettings();
                    Thread th = getThread();
                    th.start();
                } else {
                    System.out.println("No directory selected");
                }

            }
            private Thread getThread() {
                Task<Void> trace = new Task<>() {
                    @Override
                    protected Void call() {
                        TextExtractions.traceFolder(Paths.get(SySettings.PATH_TO_FOLDER.getPath()).toFile(), getControllerObject());
                        return null;
                    }
                };
                return new Thread(trace);
            }
        });
        actImportInvoices.setOnAction(actionEvent -> loadController("invoices-import-view.fxml"));
        actStatus.setOnAction(actionEvent -> loadController("status-view.fxml"));
        actPreviewInvoices.setOnAction(actionEvent -> loadController("preview-file-view.fxml"));
        actFilterInvoices.setOnAction(actionEvent -> loadController("filter-invoices-view.fxml"));
        actDbOverview.setOnAction(actionEvent -> loadController("database-overview-view.fxml"));
        actResultsOverview.setOnAction(actionEvent -> loadController("results-view.fxml"));
        actSuppConfig.setOnAction(actionEvent -> loadController("suppliers-config-view.fxml"));
        actImportTxts.setOnAction(actionEvent -> loadController("import-txts-view.fxml"));
        actProductSuppliers.setOnAction(actionEvent -> loadController("product-supplier-relations.fxml"));
        actSuppOverview.setOnAction(actionEvent -> loadController("supplier-overview.fxml"));


    }
    public void addWatcherToInvoicesFolder(){
        Path folderPath = Paths.get(SySettings.PATH_TO_FOLDER.getPath());
        try {
            // Create a WatchService
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // Register the service on the folder, specifying which kinds of events to watch
            folderPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            //File[] previousFiles = folderPath.toFile().listFiles();
            //System.out.println("hello controller before getting checksums");
            //Map<Path, File> previousFiles = getFilesInDirectory(folderPath);
            //SessionFactory sf = HibernateUtil.getSessionFactory();
            //DBErrorDAO dberdao = new DBErrorDAO(sf,new ErrorEventManager());
            //DocumentDAO ddao = new DocumentDAO(dberdao);
            //List<String> checksumList = ddao.getAllChecksums();


            // Infinite loop to watch for changes in the path
            while (true) {
                System.out.println("- - - - - - - - - - inside watchers while loop - - - - - - - - ");
                // retrieve the key for the next watch event
                WatchKey key = watchService.take();
                // Poll for file system events on the WatchKey
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    System.out.println("the kind of event "+event.kind());
                    Path changedFile = (Path) event.context();
                    Path resolvedPath = folderPath.resolve((Path) event.context());

                    if(kind==StandardWatchEventKinds.ENTRY_CREATE){
                        System.out.println("- - - - - - - - - - - - entry");
                        TextExtractions.checkFile(new File(resolvedPath.toAbsolutePath().toString()),listManager);
                        //System.out.println("file created");
                        //System.out.println(changedFile+" - - - "+resolvedPath.toAbsolutePath());
                    } else if(kind==StandardWatchEventKinds.ENTRY_MODIFY){
                        System.out.println("- - - - - - - - - - - - mod");
                        TextExtractions.checkFile(new File(resolvedPath.toAbsolutePath().toString()),listManager);
                        //System.out.println("file modified");
                    } else if (kind ==StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.println("File deleted: " + changedFile);
                        for (Document d :listManager.getToImportQueue().toList()){
                            System.out.println(d.getPath().toLowerCase().trim()+" : "+resolvedPath.toAbsolutePath().toString().toLowerCase().trim());
                            if (resolvedPath.toAbsolutePath().toString().trim().toLowerCase().compareTo(d.getPath().toLowerCase().trim())==0){
                                listManager.getToImportQueue().remove(d);
                                System.out.println("found and deleted : "+listManager.getToImportQueue().size());
                            }
                        }
                    } else {
                        System.out.println("unexpected kind of folder activity");
                    }

                }

                // Reset the key (to keep watching) - return true if the key is still valid, and false if the key could not be reset (if the directory is no longer accessible)
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static void detectRenames(File[] previousFiles, File[] currentFiles,ListManager listManager) {
        // Compare the previous and current lists of files
        for (File prevFile : previousFiles) {
            boolean found = false;
            for (File currFile : currentFiles) {
                if (prevFile.getName().equals(currFile.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // The file was in the previous list but not in the current list (possibly renamed)
                System.out.println("File renamed: " + prevFile.getName());
                for(Document d: listManager.getToImportQueue().toList()){
                    if(d.getPath().toLowerCase().trim().compareTo(prevFile.getPath().toLowerCase().trim())==0){
                        listManager.getToImportQueue().remove(d);
                    }
                }
            }
        }
    }
    private static Map<Path, File> getFilesInDirectory(Path directory) throws IOException {
        Map<Path, File> files = new HashMap<>();
        File[] fileList = directory.toFile().listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                files.put(file.toPath().getFileName(), file);
            }
        }

        return files;
    }
    private HelloController getControllerObject(){
        return this;
    }

    public void loadController(String fxmlResourceName){
        Platform.runLater(()->{
            try {
                if (this.currentChild==null){
                    System.out.println("current child is null");
                } else {
                    currentChild.removeListeners(this);
                    currentChild.setState();
                    currentChild = null;
                }

                // Load the child FXML view.
                //FXMLLoader loader = new FXMLLoader(getClass().getResource("invoices-import-view.fxml"));
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResourceName));
                Parent childView = loader.load();
                // Optionally get the child controller if needed.
                currentChild = loader.getController();
                currentChild.initialize(this);
                currentChild.addMyListeners();
                currentChild.getPreviousState();

                AnchorPane.setTopAnchor(childView, 0.0);
                AnchorPane.setRightAnchor(childView, 0.0);
                AnchorPane.setBottomAnchor(childView, 0.0);
                AnchorPane.setLeftAnchor(childView, 0.0);

                // Add the child view to the anchor pane.
                parentContainer.getChildren().setAll(childView);
                parentContainer.setMaxHeight(Double.MAX_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }
    private void displayNextMessage() {
        // Create a FadeTransition.
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.8), txtInfoMessage);
        fadeTransition.setFromValue(1.0); // Fully visible
        fadeTransition.setToValue(0.0);   // Fully transparent
        fadeTransition.setCycleCount(20);
        fadeTransition.setAutoReverse(true);


        // Start the animation.
        fadeTransition.play();
        if (!messageQueue.isEmpty()) {
            playSoundNotification();
            String nextMessage = messageQueue.poll();  // Retrieve and remove the next message
            txtInfoMessage.setText(nextMessage);  // Display the message
            notificationTimeline.playFromStart();  // Wait for 5 seconds before the next message
            timeline.play();
        } else {
            txtInfoMessage.setText("");  // No message to display
            notificationTimeline.stop();  // Stop the timeline if no more messages are in the queue
        }
    }

    public void enqueueMessage(String message) {
        Platform.runLater(()->{
            messageQueue.add(message);
            if (!notificationTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                // If the timeline is not running, start it
                displayNextMessage();
            }
        });

    }
    public void playSoundNotification(){
        Thread soundThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream("appFiles/audio/notification-sound.mp3")) {
                AdvancedPlayer player = new AdvancedPlayer(fis);
                player.play();
            } catch (JavaLayerException | IOException e) {
                e.printStackTrace();
            }
        });
        soundThread.start();
    }
}