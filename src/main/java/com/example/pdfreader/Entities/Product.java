package com.example.pdfreader.Entities;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.enums.StoreNames;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.util.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_descriptions",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "description")
    private List<String> descriptions = new ArrayList<>();

    @Column(name = "master", nullable = false)
    @NaturalId
    private String master;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StoreBasedAttributes> storeBasedAttributes = new ArrayList<>();



    public Product(ListManager listManager,String productCode,String description,String master){
        this.code = productCode;
        setDescription(description);
        this.master = master;
        //listManager.putProductToMap(this);
    }
    public Product(String description, String master){
        this.master = master;
        setDescription(description);
        //listManager.putProductToMap(this);
    }


    public Product(){
    }
    public String getMaster(){
        return master;
    }
    public String getDescription(){
        return descriptions.isEmpty() ? null : descriptions.get(descriptions.size() - 1);
    }
    public List<String> getDescriptions(){
        return descriptions;
    }
    public String getCode(){
        if(this.code!=null){
            return this.code;
        }
        return "";
    }
    public void setDescription(String description){
        if(!this.descriptions.contains(description.trim())){
            this.descriptions.add(description.trim());
        }
    }
    public void setMaster(String master){
        if(this.master==null){
            this.master = master;
            return;
        }
        if(this.master.compareTo(master)!=0){
            System.out.println("master changed for "+code);
            this.master = master;
        }
    }


    @JsonIgnore
    public int getDocCounter(){
        return 0;
        //return docEntries.size();
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<StoreBasedAttributes> getStoreBasedAttributes() {
        return storeBasedAttributes;
    }

    public void setHopeCodes(List<StoreBasedAttributes> hopecodes) {
        this.storeBasedAttributes = hopecodes;
    }

    public Long getId() {
        return id;
    }
    public boolean  checkHopeCode(String hope){
        //System.out.println("chcking hope "+hope);
        try {
            for (StoreBasedAttributes storeBasedAttributes : this.storeBasedAttributes){
                if (storeBasedAttributes.getHope().trim().compareTo(hope.trim())==0){
                    return true;
                }
            }

        } catch (Exception e){
            System.out.println("error in checking");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    /*
    public Integer getFamily(){
        for(StoreBasedAttributes storeBasedAttributes : this.storeBasedAttributes){
            if(storeBasedAttributes.getHope().trim().length()!=7 && storeBasedAttributes.getHope().trim().length()>2){
                System.out.println("i found a hope less than 7 "+ storeBasedAttributes.getHope().length()+" "+ storeBasedAttributes.getHope()+" // master:"+descriptions.get(0));
                StringBuilder sb = new StringBuilder();
                sb.append("0".repeat(Math.max(0, 7 - storeBasedAttributes.getHope().trim().length())));
                sb.append(storeBasedAttributes.getHope().trim());
                return Integer.parseInt(sb.toString().trim().substring(0,3));
            } else if (storeBasedAttributes.getHope().trim().length()<2){
                return 0;
            } else if (storeBasedAttributes.getHope().trim().length()==7){
                return Integer.parseInt(storeBasedAttributes.getHope().trim().substring(0,3));
            }
        }
        return -1;
    }
     */

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return Objects.equals(id, product.id); // Assuming 'id' is the unique identifier
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


}
