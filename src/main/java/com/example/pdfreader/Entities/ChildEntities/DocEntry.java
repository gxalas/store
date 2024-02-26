package com.example.pdfreader.Entities.ChildEntities;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Helpers.ListManager;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.*;
@Entity
@Table(name = "doc_entries", indexes = {
        @Index(name = "idx_doc_entry_document", columnList = "document_id"),
        @Index(name = "idx_doc_entry_product", columnList = "product_id")
})
public class DocEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NaturalId
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id")
    private Document document;
    @Column(name = "master")
    private String master;

    @Column(name = "code")
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "store_based_attributes_id")
    private StoreBasedAttributes sba;

    @Column(name = "boxes", precision = 9, scale = 4)
    private BigDecimal boxes;

    @Column(name = "units_per_box", precision = 9, scale = 4)
    private BigDecimal unitsPerBox;

    @Column(name = "units", precision = 9, scale = 4)
    private BigDecimal units;

    @Column(name = "unit_price", precision = 9, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 9, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "net_value", precision = 9, scale = 4)
    private BigDecimal netValue;

    @Column(name = "vat_value", precision = 9, scale = 4)
    private BigDecimal vatValue;

    @Column(name = "vat_percent", precision = 9, scale = 4)
    private BigDecimal vatPercent;

    // @JsonIgnore used to indicate that this property is not to be included in serialization
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doc_entry_error_logs", joinColumns = @JoinColumn(name = "doc_entry_id"))
    @Column(name = "error_log")
    private List<String> errorLogs = new ArrayList<>();

    @Transient
    ListManager listManager;

    public DocEntry(DocLine docLine, ListManager listManager){
        this.listManager = listManager;
        this.document = docLine.document;
        this.master = docLine.getRetEan();
        this.code = docLine.productId;
        listManager.docEntriesDescriptions.put(master,docLine.description);



        if(listManager.invoicesMasterToSba.get(master)!=null
                && listManager.invoicesMasterToSba.get(master).getStore().compareTo(document.getStore())==0){
            setSba(listManager.invoicesMasterToSba.get(master));
        } else {
            StoreBasedAttributes sba = new StoreBasedAttributes();
            sba.setMasterCode(master);
            sba.setDescription(docLine.description);
            sba.setStore(document.getStore());
            setSba(sba);
            //sba.setStore(docLine.document.getStore());
            listManager.invoicesMasterToSba.put(master,sba);
        }


        /*
        Product tempProduct = listManager.getProductHashMap().get(docLine.getRetEan());
        if (tempProduct==null){
            tempProduct = new Product(docLine.productId,docLine.description,docLine.getRetEan());
            listManager.addToProductHashMap(tempProduct);
        }
        this.product = tempProduct;
        */



        extractNumericValues(docLine);
        //product.addDocEntry(this);
    }
    public DocEntry(){

    }

    public boolean extractNumericValues(DocLine docline){
        try {
            boxes =new BigDecimal(docline.numericValues.get(2));
            unitsPerBox = new BigDecimal(docline.numericValues.get(3));
            units = new BigDecimal(docline.numericValues.get(4));
            unitPrice = new BigDecimal(docline.numericValues.get(5));

            if(!boxes.multiply(unitsPerBox).equals(units)){
                addErrorLog(boxes+" * "+unitsPerBox+" = "+units+" -> "+boxes.multiply(unitsPerBox));
            }

            totalPrice = new BigDecimal(docline.numericValues.get(6));
            if(master.compareTo("0000")!=0) { //stin periptosi pou einai metaforika (den exoume boxes kai units) den tsekaroume tin praksi
                if(units.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP).compareTo(totalPrice)!=0){
                    addErrorLog("values "+units+" * "+unitPrice+" = "+totalPrice+" => "+units.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
                }
            }
            netValue = new BigDecimal(docline.numericValues.get(7));
            vatValue = new BigDecimal(docline.numericValues.get(8));
            vatPercent = new BigDecimal(docline.numericValues.get(9));
            if(vatPercent.compareTo(new BigDecimal("100"))>0){
                addErrorLog("the vat percent is: "+vatPercent);
                //errorLogs.add();
            }
        } catch (Exception e){
            System.out.println(" - - - - - - - - - - - - - - - - - - - - - - -");
            System.err.println("the exception happened "+e);
            System.out.println(docline.numericValues);
            System.out.println("the values 2: "+boxes+" "+unitsPerBox+" "+units+" "+unitPrice+" "+totalPrice+" "+netValue+" "+vatValue+" "+vatPercent);
        }
        return errorLogs.isEmpty();
    }

    public String getMaster(){
        return this.master;
    }

    @JsonIgnore
    public String getCode(){
        return this.code;
    }
    public BigDecimal getBoxes() {
        if(boxes==null){
            return BigDecimal.ZERO;
        }
        return boxes;
    }
    public BigDecimal getUnitsPerBox() {
        if(unitsPerBox==null){
            return BigDecimal.ZERO;
        }
        return unitsPerBox;
    }
    public BigDecimal getUnits() {
        if(units==null){
            return BigDecimal.ZERO;
        }
        return units;
    }
    public BigDecimal getUnitPrice() {
        if(unitPrice==null){
            return BigDecimal.ZERO;
        }
        return unitPrice;
    }
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    public BigDecimal getNetValue() {
        return netValue;
    }
    public BigDecimal getVatValue() {
        return vatValue;
    }
    public BigDecimal getVatPercent() {
        return vatPercent;
    }
    public List<String> getErrorLogs() {
        return errorLogs;
    }
    @JsonIgnore
    public BigDecimal getMixPrice(){
        return netValue.add(vatValue);
    }

    public void setVatPercent(BigDecimal value){
        vatPercent = value;
    }
    public void setNetValue(BigDecimal value){
        netValue = value;
    }
    public void setVatValue(BigDecimal value){
        vatValue = value;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setUnitsPerBox(BigDecimal unitsPerBox) {
        this.unitsPerBox = unitsPerBox;
    }

    public void setBoxes(BigDecimal boxes) {
        this.boxes = boxes;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @JsonIgnore
    public Product getProduct() {
        if(sba.getProduct()==null){
            Product product1 = new Product();
            product1.setInvmaster("error");
            product1.setCode("error");
            product1.setInvDescription("error");
            return product1;
        } else {
            return sba.getProduct();
        }


        //return product;
    }

    private void addErrorLog(String text){
        errorLogs.add(text);
    }


    @JsonIgnore
    public Date getDate(){
        return document.getDate();
    }

    public void setDocumentId(String documentId) {
    }
    public void setDocument(Document doc){
        this.document = doc;
    }

    public Document getDocument() {
        return document;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public void setCode(String c){
        this.code = c;
    }
    public StoreBasedAttributes getSba(){
        return this.sba;
    }

    public void setSba(StoreBasedAttributes sba) {
        this.sba = sba;
    }
}
