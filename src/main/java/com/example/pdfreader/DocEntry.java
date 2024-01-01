package com.example.pdfreader;

import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

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


    // Assume that DocLine and listManager are part of the logic to create a DocEntry, which should not be needed as JPA will handle this.
    // Remove or adapt constructor logic accordingly if needed.
    public DocEntry(Document document, Product product, BigDecimal boxes, BigDecimal unitsPerBox,
                    BigDecimal units, BigDecimal unitPrice, BigDecimal totalPrice,
                    BigDecimal netValue, BigDecimal vatValue, BigDecimal vatPercent) {
        this.document = document;
        this.product = product;
        this.boxes = boxes;
        this.unitsPerBox = unitsPerBox;
        this.units = units;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.netValue = netValue;
        this.vatValue = vatValue;
        this.vatPercent = vatPercent;
    }
    public DocEntry(DocLine docLine, ListManager listManager){
        this.listManager =listManager;
        document = docLine.document;
        Product tempProduct = listManager.getProductHashMap().get(docLine.getRetEan());
        if (tempProduct==null){
            tempProduct = new Product(listManager,docLine.productId,docLine.description,docLine.getRetEan());
            listManager.addToProductHashMap(tempProduct);
        }
        this.product = tempProduct;

        if(getProduct()!=null){
            getProduct().setDescription(docLine.description.trim());
            getProduct().setMaster(docLine.getRetEan());
        }

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
            if(product.getMaster().compareTo("0000")!=0) { //stin periptosi pou einai metaforika (den exoume boxes kai units) den tsekaroume tin praksi
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
    @JsonIgnore
    public String getDescription(){
        return product.getDescription();
    }

    public String getProductMaster(){
        return product.getMaster();
    }

    @JsonIgnore
    public String getProductCode(){
        return product.getCode();
    }
    public BigDecimal getBoxes() {
        return boxes;
    }
    public BigDecimal getUnitsPerBox() {
        return unitsPerBox;
    }
    public BigDecimal getUnits() {
        return units;
    }
    public BigDecimal getUnitPrice() {
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
        if(this.product==null){
            return new Product();
        }
        return product;
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

}
