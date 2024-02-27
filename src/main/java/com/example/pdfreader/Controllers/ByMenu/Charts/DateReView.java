package com.example.pdfreader.Controllers.ByMenu.Charts;

import com.example.pdfreader.Controllers.ChildController;
import com.example.pdfreader.DAOs.PosEntryDAO;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Sinartiseis.HelpingFunctions;
import com.example.pdfreader.enums.StoreNames;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateReView extends ChildController {
    public DatePicker datePicker;
    public ComboBox<StoreNames> cbStore;
    public TableView<PosEntry> tableResults = new TableView<>();
    public Text txtResult;
    private ObservableList<PosEntry> obsPosEntries = FXCollections.observableArrayList();

    @Override
    public void initialize(HelloController hc) {
        this.parentDelegate = hc;
        List<StoreNames> cbOptions = new ArrayList<>();
        StoreNames.stringValues().forEach(name->{
            cbOptions.add(StoreNames.getStoreByName(name));
        });
        //cbOptions.add(StoreNames.DRAPETSONA);
        //cbOptions.add(StoreNames.PERISTERI);
        cbStore.setItems(FXCollections.observableList(cbOptions));
        cbStore.getSelectionModel().select(0);

        initTableResults();

        datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observableValue, LocalDate localDate, LocalDate t1) {
                System.out.println(" a new date is selected for store "+cbStore.getSelectionModel().getSelectedItem().getName());
                getResults();
            }
        });
        cbStore.valueProperty().addListener(new ChangeListener<StoreNames>() {
            @Override
            public void changed(ObservableValue<? extends StoreNames> observableValue, StoreNames storeNames, StoreNames t1) {
                getResults();
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

    private void initTableResults(){
        TableColumn<PosEntry, String>  dateCol = new TableColumn<>("date");
        dateCol.setCellValueFactory(cellData->{
            Date date = cellData.getValue().getDate();
            return new ReadOnlyObjectWrapper<>(HelpingFunctions.format.format(date));
        });

        TableColumn<PosEntry,String> descriptionCol = new TableColumn<>("description");
        descriptionCol.setCellValueFactory(cellData->{
            return new ReadOnlyStringWrapper(cellData.getValue().getDescription());
        });

        TableColumn<PosEntry,Number> quantityCol = new TableColumn<>("quantity");
        quantityCol.setCellValueFactory(cellData->{
            return new ReadOnlyObjectWrapper<>(cellData.getValue().getQuantity());
        });

        TableColumn<PosEntry,Number> moneyCol = new TableColumn<>("money");
        moneyCol.setCellValueFactory(cellData->{
            return new ReadOnlyObjectWrapper<>(cellData.getValue().getMoney());
        });

        TableColumn<PosEntry,String> departmentCol = new TableColumn<>("department");
        departmentCol.setCellValueFactory(cellData->{
            return new ReadOnlyObjectWrapper<>(cellData.getValue().getSba().getDepartment());
        });

        TableColumn<PosEntry,String> hopeCol = new TableColumn<>("hope");
        hopeCol.setCellValueFactory(cellData->{
            return new ReadOnlyStringWrapper(cellData.getValue().getSba().getHope());
        });

        TableColumn<PosEntry,Number> priceCol = new TableColumn<>("price");
        priceCol.setCellValueFactory(cellData->{
            return new ReadOnlyObjectWrapper<>(cellData.getValue().getPrice());
        });

        tableResults.getColumns().setAll(dateCol,descriptionCol,quantityCol,
                priceCol,moneyCol, departmentCol,hopeCol);
        tableResults.setItems(obsPosEntries);
    }

    private void getResults(){
        if(datePicker.getValue()==null){
            return;
        }
        MyTask task = new MyTask(()->{

            Date date = HelpingFunctions.convertLocalDateToDate(datePicker.getValue());
            StoreNames store = cbStore.getValue();

            PosEntryDAO posEntryDAO = new PosEntryDAO();
            List<PosEntry> posEntries = posEntryDAO.getPosEntriesByDateAndStoreName(date,store);

            obsPosEntries.setAll(posEntries);

            final BigDecimal[] sum = {BigDecimal.ZERO};
            posEntries.forEach(posEntry -> {
                sum[0] = sum[0].add(posEntry.getMoney());
            });
            txtResult.setText(sum[0]+"");
            return null;
        });


        parentDelegate.listManager.addTaskToActiveList(
                "get results for a date",
                datePicker.getValue()+" for "+cbStore.getValue().getName(),
                task
        );
    }
}
