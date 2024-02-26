package com.example.pdfreader.Entities.ChildEntities;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.TypesOfDocuments.ABUsualInvoice;
import com.example.pdfreader.enums.StoreNames;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Entity
@Table(name = "pos_entries", indexes = {
        @Index(name = "idx_pos_entry_product", columnList = "product_id"),
        @Index(name = "idx_pos_entry_date", columnList = "entry_date")
})
public class PosEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_name")
    public StoreNames storeName;

    @Column(name = "entry_date")
    @Temporal(TemporalType.TIMESTAMP) // or TemporalType.DATE based on your requirement
    public Date date;

    private String master;

    @Column(length = 35)
    public String description;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "store_based_attributes_id")
    private StoreBasedAttributes sba;

    @Column(length = 5)
    public String fpaCode;

    @Column(length = 7)
    public String box;

    @Column(precision = 10, scale = 2)
    public BigDecimal money;

    @Column(precision = 10, scale = 2)
    public BigDecimal quantity;

    @Column(precision = 10, scale = 2)
    public BigDecimal fpaPercent;

    @Column(name = "sha_code", unique = true)
    private String shaCode;


    static int counter = 1;


    public PosEntry(){}


    public PosEntry(String line, StoreNames store){
        this.storeName = store;
        this.date = retDate(line);
        this.master = line.substring(8,19);
        this.description = line.substring(19, 54);
        this.fpaCode = line.substring(54,55);
        this.box = line.substring(55,62);
        try {
            this.money = new BigDecimal(line.substring(62, 72).trim());
        } catch (NumberFormatException e) {
            System.err.println("error here m: \n "+line.substring(62, 72).trim());
            this.money = BigDecimal.ZERO;
            // Handle the case where stringValue is not a valid number
            // For example, log an error, set a default value, or skip this record
        }
        try {
            this.quantity= new BigDecimal(line.substring(72,81).trim());
        } catch (NumberFormatException e) {
            System.err.println("error here q: \n "+line.substring(72,81).trim());
            this.quantity = BigDecimal.ZERO;
            // Handle the case where stringValue is not a valid number
            // For example, log an error, set a default value, or skip this record
        }
        try {
            this.fpaPercent= new BigDecimal(line.substring(81).trim());
        } catch (NumberFormatException e) {
            System.err.println("error here fp: \n "+line.substring(81).trim());
            this.fpaPercent = BigDecimal.ONE;
            // Handle the case where stringValue is not a valid number
            // For example, log an error, set a default value, or skip this record
        }

    }
    public Date retDate(String line){
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR,Integer.parseInt(line.substring(0,4)));
        calendar.set(Calendar.MONTH,Integer.parseInt(line.substring(4,6))-1);
        calendar.set(Calendar.DATE,Integer.parseInt(line.substring(6,8)));
        return calendar.getTime();
    }
    public String getDescription(){
        return description;
    }
    public Date getDate(){
        return date;
    }
    public BigDecimal getQuantity(){
        return quantity;
    }
    public BigDecimal getMoney(){
        return money;
    }

    @JsonIgnore
    public BigDecimal getPrice(){
        if(quantity.compareTo(BigDecimal.ZERO)==0){
            System.out.println("zero entry : "+this.getSba().getDescription()+" date : "+this.getDate());
            return BigDecimal.ZERO;
        }
        return money.setScale(2,RoundingMode.HALF_UP).divide(quantity, RoundingMode.HALF_UP);
    }

    public String getMaster(){
        return this.master;
    }

    public void setProduct(Product p){
        this.product = p;
    }
    public Product getProduct(){
        return this.product;
    }

    public Long getId() {
        return id;
    }
    public String getShaCode(){
        return shaCode;
    }
    public void setShaCode(String code){
        this.shaCode = code;
    }

    public void setSba(StoreBasedAttributes sba) {
        this.sba = sba;
    }

    public StoreBasedAttributes getSba() {
        return sba;
    }

    public StoreNames getStoreName() {
        return storeName;
    }

    public String sha256(int index) {

        String t = index+ABUsualInvoice.format.format(date)+master+storeName.getDescription();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(t.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            if (counter>0){
                System.out.println("this is for sha encoding test"+t+" : "+hexString.toString());
                counter--;
            }

            String result = hexString.toString();



            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


}
