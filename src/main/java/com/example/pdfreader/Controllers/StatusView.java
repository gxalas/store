package com.example.pdfreader.Controllers;

import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Interfaces.ParentControllerDelegate;
import com.example.pdfreader.enums.SySettings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class StatusView extends ChildController {
    @FXML
    public TextArea txtAMemoryStatus;
    @FXML
    public Text txtFolderLocation;

    private final ListChangeListener<String> textAreaStatusUpdater = new ListChangeListener<String>() {
        @Override
        public void onChanged(Change<? extends String> change) {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (String s : change.getAddedSubList()) {
                        txtAMemoryStatus.appendText(s + "\n");
                    }
                }
            }
            System.out.println("listener has happened");
        }
    };
    private final ChangeListener<String> txtFolderLocationUpdater = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            txtFolderLocation.setText("Folder Location : " + SySettings.PATH_TO_FOLDER.path.get());
            System.out.println("the folder path has changed");
        }
    };

    @FXML
    public void initialize(){

    }
    @Override
    public void initialize(HelloController hc) {
        super.parentDelegate = hc;
        for(String txt:hc.txtAMemoryStatusList){
            txtAMemoryStatus.appendText(txt);
        }
        txtFolderLocation.setText("Folder Location: " + SySettings.PATH_TO_FOLDER.getPath());
    }



    @Override
    public void addMyListeners() {
        parentDelegate.actStatus.setDisable(true);

        //Listener gia otan i lista me ta txt pou prepei na emfanizountai sto textArea
        //allazei -> prostithetai i allagi sto textArea
        parentDelegate.txtAMemoryStatusList.addListener(textAreaStatusUpdater);

        //Listener gia otan allazei o fakelos apo ton opoio diavazoume ta arxeia
        //allazei -> enimeronetai to text stin selida status
        SySettings.PATH_TO_FOLDER.path.addListener(txtFolderLocationUpdater);
    }

    @Override
    public void removeListeners(HelloController hc) {
        hc.actStatus.setDisable(false);
        hc.txtAMemoryStatusList.removeListener(textAreaStatusUpdater);
        SySettings.PATH_TO_FOLDER.path.removeListener(txtFolderLocationUpdater);
    }

    @Override
    public <T extends ChildController> T getControllerObject() {
        return (T)this;
    }

    @Override
    public void setState() {

    }

    @Override
    public void getPreviousState() {

    }
}
