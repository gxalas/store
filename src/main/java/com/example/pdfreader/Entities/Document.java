package com.example.pdfreader.Entities;

import com.example.pdfreader.DAOs.DBErrorDAO;
import com.example.pdfreader.DAOs.DocumentDAO;
import com.example.pdfreader.DAOs.SupplierProductRelationDAO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.DocumentErrors;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import com.example.pdfreader.Sinartiseis.TextExtractions;
import com.example.pdfreader.enums.ABInvoiceTypes;
import com.example.pdfreader.enums.PromTypes;
import com.example.pdfreader.enums.StoreNames;
import com.fasterxml.jackson.annotation.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;


@Entity
@NamedEntityGraph(name = "document.entries.product",
        attributeNodes = {
                @NamedAttributeNode(value = "entries", subgraph = "entries.product")
        },
        subgraphs = {
                @NamedSubgraph(name = "entries.product",
                        attributeNodes = @NamedAttributeNode("product"))
        }
)
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_id", columnList = "document_id")
})
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "document_id")
    @NaturalId
    private String documentId = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "store")
    private StoreNames store = StoreNames.PERISTERI;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ABInvoiceTypes type = ABInvoiceTypes.TIMOLOGIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "prom_type")
    private PromTypes promType = PromTypes.AB;
    @ManyToOne
    @JoinColumn(name = "supplier_id") // This is the foreign key in the Document table.
    private Supplier supplier;

    @Column(name = "sub_type")
    private String subType = "";

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @BatchSize(size=50)
    private List<DocEntry> entries = new ArrayList<>();

    //@Column(name = "error_list")
    //@ElementCollection(fetch = FetchType.EAGER)
    //private List<String> errorList = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "document_errors_id") // This is the foreign key column
    private DocumentErrors errors;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "import_date")
    private Date importDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "date")
    private Date date = Calendar.getInstance().getTime();

    @Column(name = "checksum")
    private String checksum;
    @ElementCollection
    @CollectionTable(name = "document_parts", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "part")
    @Lob
    private List<String> parts = new ArrayList<>();

    @Column(name = "unit_sum")
    private BigDecimal unitSum = BigDecimal.ZERO;

    @Column(name = "val_sum")
    private BigDecimal valSum = BigDecimal.ZERO;

    @Column(name = "net_sum")
    private BigDecimal netSum = BigDecimal.ZERO;

    @Column(name = "vat_sum")
    private BigDecimal vatSum = BigDecimal.ZERO;

    @Column(name = "sum_retrieved")
    private BigDecimal sumRetrieved = BigDecimal.ZERO;

    @Column(name = "sum_entries")
    private BigDecimal sumEntries = BigDecimal.ZERO;

    // default constructor
    public Document() {
        //defaultPath
    }
    public Document(String path){
        this.filePath = path;
        importDate = Calendar.getInstance().getTime();
        //this.checksum = TextExtractions.calculateChecksum(this.path);
    }



    /**
     * get the contents of the pdf file using iText
     */

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public String getDocumentId(){
        return documentId;
    }
    @JsonIgnore
    public String getFileName(){
        return new File(filePath).getName();
    }
    public String getPath(){
        return filePath;
    }
    public Date getDate(){
        return this.date;
    }
    public void setDate(Date date){
        this.date = date;
    }
    public ABInvoiceTypes getType(){
        return type;
    }
    public void setType(ABInvoiceTypes type){
        this.type = type;
    }
    public String getDuplicate(){
        //return "hello";

        if(errors==null){
            errors=new DocumentErrors(new ArrayList<>());
        }
        return String.valueOf(errors.getErrorMessages().contains("duplicate"));
    }
    public String getChecksum(){
        if (this.checksum==null){
            this.checksum = TextExtractions.calculatePDFChecksum(new File(filePath));
        }
        return checksum;
    }
    public void setChecksum(String value){
        this.checksum = value;
    }
    public PromTypes getPromType(){
        return promType;
    }
    public void setPromType(PromTypes prom){
        this.promType = prom;
    }
    public StoreNames getStore(){
        return store;
    }
    public void setStore(StoreNames store){
        this.store = store;
    }
    public BigDecimal getSumEntries(){
        return sumEntries;
    }
    public void setSumEntries(BigDecimal val){
        this.sumEntries = val;
    }
    public BigDecimal getSumRetrieved(){
        return sumRetrieved;
    }
    public void setSumRetrieved(BigDecimal val){
        this.sumRetrieved = val;
    }
    public BigDecimal getSumDiff(){
        return sumRetrieved.subtract(sumEntries);
    }
    public List<String> getErrorList(){
        if(errors==null){
            errors  = new DocumentErrors(new ArrayList<>());
        }
        return errors.getErrorMessages();
    }
    public void setErrorList(List<String> list){
        errors = new DocumentErrors(list);
    }
    public void addToErrorList(String error){
        if(errors==null){
            errors = new DocumentErrors(new ArrayList<>());
        }
        errors.addErrorMessage(error);
    }
    public Date getImportDate(){
        return importDate;
    }
    public void setImportDate(Date date){
        this.importDate = date;
    }
    public List<DocEntry> getEntries(){
        return entries;
    }
    public void setEntries(List<DocEntry> entries){
        this.entries = new ArrayList<DocEntry>(entries);
    }
    public BigDecimal getUnitSum(){
        return this.unitSum;
    }
    public void setUnitSum(BigDecimal value){
        this.unitSum = value;
    }
    public BigDecimal getValSum(){
        return this.valSum;
    }
    public void setValSum(BigDecimal value){
        this.valSum = value;
    }
    public BigDecimal getNetSum(){
        return this.netSum;
    }
    public void setNetSum(BigDecimal value){
        this.netSum = value;
    }
    public BigDecimal getVatSum(){
        return this.vatSum;
    }
    public void setVatSum(BigDecimal value){
        this.vatSum = value;
    }
    public static Document getDocumentByChecksum (ListManager listManager, String checksum){
        for(Document doc:listManager.getImported()){
            if(doc.checksum.compareTo(checksum)==0){
                return doc;
            }
        }
        return null;
    }

    public void updateDocumentsFile(String path,ListManager listManager){
        if(this.filePath.compareTo(path)!=0){
            Document duplicated = new Document(this.filePath);
            //this.errorList.add("duplicate");
            //duplicated.errorList.add("duplicate");
            listManager.addToFailed(duplicated);
            this.filePath = path;
        }
    }
    public boolean getHasError(){
        if(errors == null ){
            errors = new DocumentErrors(new ArrayList<>());
        }
        //return false;
        return !errors.getErrorMessages().isEmpty();
    }
    public void setSubType(String subType){
        this.subType = subType;
    }
    public String getSubType(){
        return this.subType;
    }
    public void setDocEntries(List<DocEntry> list){
        entries = list;
    }
    public String getFilePath(){
        return this.filePath;
    }
    public List<String> getParts(){
        return parts;
    }
    public void setParts(List<String> parts){
        this.parts  = parts;
    }


    public List<Product> getProducts(){
        List<Product> products = new ArrayList<>();
        for(DocEntry docEntry:entries){
            if(docEntry.getProduct()!=null){
                products.add(docEntry.getProduct());
            }
        }
        return products;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public static List<SupplierProductRelation> inferSupplier(List<SupplierProductRelation> currentRelations, Document newDoc) {
        if (newDoc.getType() != ABInvoiceTypes.TIMOLOGIO) {
            return Collections.emptyList();
        }
        if(newDoc.getDocumentId().compareTo("9033568261")==0){
            System.out.println("this is the document in question");
            System.out.println("the current relations are : "+currentRelations.size());
            newDoc.getProducts().forEach(product->{
                System.out.print("the product "+product.getDescription()+", ");
                currentRelations.forEach(relation->{
                    if(relation.getProduct().getMaster().compareTo(product.getMaster())==0){
                        System.out.print(" a supplier is : "+relation.getSupplier().getName()+", ");
                        System.out.println();
                    }
                });
            });
        }


        List<SupplierProductRelation> newRelations = new ArrayList<>();

        // Convert relations to a Map for easy access
        /*
        Map<Product, Set<Supplier>> productSupplierMap = currentRelations.stream()
                .collect(Collectors.groupingBy(
                        SupplierProductRelation::getProduct,
                        Collectors.mapping(SupplierProductRelation::getSupplier, Collectors.toSet())
                ));
         */


        Map<Product,List<Supplier>> productSupplierMap  = currentRelations.stream()
                .collect(Collectors.groupingBy(
                        SupplierProductRelation::getProduct,
                        Collectors.mapping(SupplierProductRelation::getSupplier, Collectors.toList())
                ));

        Map<Supplier, Integer> supplierFrequency = new HashMap<>();
        for (DocEntry entry : newDoc.getEntries()) {
            Product product = entry.getProduct();
            List<Supplier> suppliers = productSupplierMap.getOrDefault(product,new ArrayList<>());
            for (Supplier supplier : suppliers) {
                supplierFrequency.put(supplier, supplierFrequency.getOrDefault(supplier, 0) + 1);
            }
        }
        //System.out.println();

        // Find the most common supplier
        Optional<Supplier> mostCommonSupplier = supplierFrequency.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);

        if (mostCommonSupplier.isPresent()) {
            Supplier supplier = mostCommonSupplier.get();
            newDoc.setSupplier(supplier);

            for (DocEntry entry : newDoc.getEntries()) {
                Product product = entry.getProduct();
                if(!productSupplierMap.containsKey(product)){

                    System.out.println("new relation at doc "+newDoc.getDocumentId()+"for product "+product.getDescription()+", and supplier "+supplier.getName()+" .");
                    SupplierProductRelation newRelation = new SupplierProductRelation(product, supplier);
                    newRelations.add(newRelation);
                    productSupplierMap.computeIfAbsent(product, k -> new ArrayList<>()).add(supplier);
                } else {
                    if (!productSupplierMap.get(product).contains(supplier)) {
                        System.out.println("new relation at doc "+newDoc.getDocumentId()+"for product "+product.getDescription()+", and supplier "+supplier.getName()+" .");
                        SupplierProductRelation newRelation = new SupplierProductRelation(product, supplier);
                        newRelations.add(newRelation);
                        productSupplierMap.computeIfAbsent(product, k -> new ArrayList<>()).add(supplier);
                    }
                }
            }
        }

        if (!newRelations.isEmpty()) {
            System.out.println("New relations to be added: " + newRelations.size());
        }
        if(newDoc.getDocumentId().compareTo("9033568261")==0){
            System.out.println("the document in question ending processing");
            System.out.println("the new relations for the document are : "+newRelations.size());
        }

        return newRelations;
    }
}
