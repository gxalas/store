package com.example.pdfreader.Entities.Attributes;


import com.example.pdfreader.Controllers.ImportTxtsView;
import com.example.pdfreader.DAOs.HibernateUtil;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.enums.StoreNames;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/*
Product Class attribute
 */
@Entity
@Table(name = "store_based_attributes", indexes = {
        @Index(name = "idx_master_code", columnList = "masterCode"),
        @Index(name = "idx_store", columnList = "store"),
        @Index(name = "idx_department", columnList = "department"),
        @Index(name = "idx_product_id", columnList = "product_id")
})
public class StoreBasedAttributes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hope")
    private String hope = "";
    @Column(name = "department")
    private String department = "";
    @Column(name = "family")
    private String family = "";
    @Enumerated(EnumType.STRING)
    @Column(name = "store")
    private StoreNames store = StoreNames.PERISTERI;
    @Column(name="masterCode")
    private String masterCode;
    @Column(name="description")
    private String description;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id",nullable = true)
    private Product product;

    @ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(
            name = "sba_barcodes",
            joinColumns = @JoinColumn(name = "store_based_attributes_id")
    )
    @Column(name = "barcodes")
    private List<String> barcodes = new ArrayList<>();
    @ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(
            name = "sba_conflicting_barcodes",
            joinColumns = @JoinColumn(name = "store_based_attributes_id")
    )
    @Column(name = "conflicting_barcodes")
    private List<String> conflictingBarcodes = new ArrayList<>();

    @Column(name="has_conflict")
    private boolean hasConflict=false;

    public StoreBasedAttributes(){}
    public StoreBasedAttributes(String hope, StoreNames store){
        setHope(hope.trim());
        //this.product = product;
        this.store = store;
    }

    //public Product getProduct() {
      //  return product;
    //}

    //public void setProduct(Product product) {
        //this.product = product;
    //}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHope() {
        return hope;
    }

    public void setHope(String hope) {
        this.hope = hope;
        if(hope.length()!=7 && hope.length()>2){
            //System.out.println("i found a hope less than 7 "+ hope.length()+" "+ hope+);
            StringBuilder sb = new StringBuilder();
            sb.append("0".repeat(Math.max(0, 7 - hope.length())));
            sb.append(hope);
            setFamily(sb.substring(0,3));
            this.hope = sb.toString();
        } else if (hope.length()<2){
            setFamily("0");
        } else if (hope.length()==7){
            setFamily(hope.substring(0,3));
        } else {
            setFamily("-12");
        }
    }

    public StoreNames getStore() {
        return store;
    }

    public void setStore(StoreNames store) {
        this.store = store;
    }

    public String getDepartment() {
        return department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        //System.out.println(" - - - ^ ^ ^  ^ ^ ^ ^ ^ ^ ^ ^ ^ ^^ "+family+ " // "+hope);
        this.family = family;
    }

    public String getMasterCode(){
        return this.masterCode;
    }

    public void setMasterCode(String master){
        this.masterCode = master;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    public Product getProduct(){
        return product;
    }

    public List<String> getBarcodes(){
        return this.barcodes;
    }

    public boolean getHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(boolean hasConflict) {
        this.hasConflict = hasConflict;
    }

    public List<String> getConflictingBarcodes() {
        return conflictingBarcodes;
    }

    public void fixConflictingBarcode(String barcode){
        if(getBarcodes().contains(barcode)){
            getBarcodes().remove(barcode);
            if(getConflictingBarcodes().contains(barcode)){
                System.out.println("the barcode already exists on the conflicting list");
            } else {
                getConflictingBarcodes().add(barcode);
            }
        } else {
            System.out.println("there is an error ");
        }
    }

    public boolean areEqual(StoreBasedAttributes sba){

        if(sba.getStore().compareTo(this.store)!=0){
            return false;
        }
        if(sba.getMasterCode().compareTo(this.masterCode)!=0){
            return false;
        }
        return true;
    }




    // Getters and setters...
}

