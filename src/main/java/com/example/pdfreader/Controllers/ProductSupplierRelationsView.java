package com.example.pdfreader.Controllers;

import com.example.pdfreader.DAOs.SupplierProductRelationDAO;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.MyTask;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class ProductSupplierRelationsView extends ChildController{
    public TableView<SupplierProductRelation> tableRelations;
    public TextField txtFilter;
    private ObservableList<SupplierProductRelation> obsRelations = FXCollections.observableArrayList();
    private List<SupplierProductRelation> allRelations = new ArrayList<>();

    @Override
    public void initialize(HelloController hc) {
        txtFilter.setOnAction(event->{
            if(txtFilter.getText().compareTo("")!=0){
                List<SupplierProductRelation> temp = new ArrayList<>();
                for(SupplierProductRelation spr:allRelations){
                    if(spr.getProduct().getMaster().compareTo(txtFilter.getText())==0){
                        temp.add(spr);
                    }
                }
                obsRelations.setAll(temp);
            }

        });

        TableColumn<SupplierProductRelation,String> productDescCol = new TableColumn<>("description");
        productDescCol.setCellValueFactory(cellData->{
            String d = " no product";
            if(cellData.getValue().getProduct()!=null){
                d = cellData.getValue().getProduct().getDescription();
            }
            return new ReadOnlyStringWrapper(d);
        });

        TableColumn<SupplierProductRelation,String> masterCol = new TableColumn<>("master");
        masterCol.setCellValueFactory(cellData->{
            String m = " no master";
            if(cellData.getValue().getProduct()!=null){
                m = cellData.getValue().getProduct().getMaster();
            }
            return new ReadOnlyStringWrapper(m);
        });

        TableColumn<SupplierProductRelation,String> supplierCol = new TableColumn<>("supplier");
        supplierCol.setCellValueFactory(cellData->{
            String s ="no supplier";
            if(cellData.getValue().getSupplier()!=null){
                s = cellData.getValue().getSupplier().getName();
            }
            return new ReadOnlyStringWrapper(s);
        });

        tableRelations.getColumns().setAll(masterCol,productDescCol,supplierCol);
        tableRelations.setItems(obsRelations);

        MyTask getRelations = new MyTask(()->{
            SupplierProductRelationDAO supplierProductRelationDAO = new SupplierProductRelationDAO();
            allRelations.addAll(supplierProductRelationDAO.findAll());
            obsRelations.setAll(allRelations);
            return null;
        });

        hc.listManager.addTaskToActiveList(
                "getting relations",
                "getting all the relations from the databas",
                getRelations
        );


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
