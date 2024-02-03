package com.example.pdfreader;

import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
import com.example.pdfreader.Sinartiseis.ProcessingTxtFiles;
import com.example.pdfreader.Sinartiseis.Serialization;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.SySettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

public class HelloApplication extends Application {
    private final ListManager listManager = new ListManager();
    public static Date minDate;
    public static Date maxDate;
    @Override
    public void start(Stage stage) throws IOException {
        System.setProperty("jna.library.path", "/usr/local/lib");

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        HelloController mainController = fxmlLoader.getController();

        mainController.setListManager(listManager);


        stage.setScene(scene);
        stage.setOnCloseRequest(windowEvent -> {
            Platform.exit();
            System.exit(0);
        });

        stage.show();

        //mainController.loadController("database-overview-view.fxml");
        mainController.loadController("import-txts-view.fxml");
        //mainController.loadController("reading-txts-view.fxml");

        /*
         * - settingsTask
         * |- docCheckSums
         *      |- trace
         *      |- readTxtFiles
         */

        MyTask settingsTask = new MyTask(()->{
            HelpingFunctions.createFileIfNotExists("appFiles/saved");
            ProcessingTxtFiles.loadSettings();                                 //load the paths for where the documents are
            return null;
        });

        MyTask trace = new MyTask(()->{
            ProcessingTxtFiles.traceFolder(Paths.get(
                    SySettings.PATH_TO_FOLDER.getPath()).toFile(),
                    fxmlLoader.getController());                                //find the files that can be imported
            return null;
        });
        trace.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                System.out.println("the tracing failed");
                System.out.println(workerStateEvent.getSource().getMessage());
                workerStateEvent.getSource().getException().printStackTrace();
            }
        });

        MyTask fetchDocumentChecksums = new MyTask(()->{
            listManager.fetchChecksums();
            return null;
        });

        MyTask readTxtFilesTask = new MyTask(()->{
            //Serialization.readTxtFiles(fxmlLoader.getController());                 //load the saved sales
            return null;
        });



        listManager.addTaskToActiveList(
                "import setting",
                "reading the current folder for documents",
                settingsTask);

        fetchDocumentChecksums.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                listManager.addTaskToActiveList(
                        "tracing",
                        "checking the folder for not imported documents",
                        trace
                );

                listManager.addTaskToActiveList(
                        "Reading the Txt files",
                        "Reading the Txt Files",
                        readTxtFilesTask
                );
            }
        });

        settingsTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                listManager.addTaskToActiveList(
                        "Fetching Documents Checksums",
                        "loading the checksums of the documents currently at DB",
                        fetchDocumentChecksums
                );
            }
        });
    }
    public static void main(String[] args) {
        launch();
    }

}