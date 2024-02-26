package com.example.pdfreader.Controllers.ByMenu.Charts;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
import com.example.pdfreader.enums.StoreNames;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DateReView extends ChildController {
    public DatePicker datePicker;
    public ComboBox<StoreNames> cbStore;

    @Override
    public void initialize(HelloController hc) {
        List<StoreNames> cbOptions = new ArrayList<>();
        cbOptions.add(StoreNames.DRAPETSONA);
        cbOptions.add(StoreNames.PERISTERI);
        cbStore.setItems(FXCollections.observableList(cbOptions));
        cbStore.getSelectionModel().select(0);

        datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observableValue, LocalDate localDate, LocalDate t1) {
                System.out.println(" a new date is sleected for store "+cbStore.getSelectionModel().getSelectedItem().getName());
                PosEntryDAO posEntryDAO = new PosEntryDAO();
                List<PosEntry> posEntries = posEntryDAO.getPosEntriesByDateAndStoreName(HelpingFunctions.convertLocalDateToDate(datePicker.getValue()),
                        cbStore.getValue());
                posEntries.forEach(posEntry -> {
                    System.out.println(posEntry.getDate()+" "+posEntry.getDescription()+" "+posEntry.getMoney()+" "+posEntry.getMaster()+" "+posEntry.getSba().getMasterCode());
                });
            }
        });
    }

    @Override
    public void addMyListeners() {

    }

    @Override
    public void removeListeners(HelloController hc) {

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
}
