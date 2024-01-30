package com.example.pdfreader.Entities;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.enums.ConflictStates;
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

    /**
     * this i think i have to remove
     */
    @Column(name = "code")
    private String code;

    /**
     * this will be changed
     * it will be the invoice description
     */
    @ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_descriptions",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "description")
    private List<String> descriptions = new ArrayList<>();


    /**
     * this will be moved
     * to the store based
     * attributes
     */


    @Column(name = "master")
    private String master;

    @Column(name = "state")
    private ConflictStates state = ConflictStates.PENDING;
    @Column(name = "description")
    private String invDescription;

    public Product(ListManager listManager,String productCode,String description,String master){
        this.code = productCode;
        if(description.contains("PAL MAL ΚΟΚΚΙΝΟ")){
            System.out.println("at Product");
        }
        this.invDescription = description;
        setDescription(description);
        this.master = master;
    }
    public Product(String description, String master){
        this.master = master;
        this.invDescription = description;
        if(description.contains("PAL MAL ΚΟΚΚΙΝΟ")){
            System.out.println("at other product");
        }
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
        if(description.contains("PAL MAL ΚΟΚΚΙΝΟ")){
            System.out.println("\n\n\n\n\n");
            System.out.println("we found pal mal red");
            System.out.println("\n\n\n\n\n");
        }
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

    public Long getId() {
        return id;
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

    /*
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;

        if (Objects.equals(master, product.master)){
            System.out.println("same master");
            if(!Objects.equals(id, product.id)){
                System.out.println(" ** we have different id "+product.master+" ids : "+((Product) obj).id+" :: "+product.id);
            }
        }
        return Objects.equals(master, product.master); // Assuming 'id' is the unique identifier
    }
     */

    @Override
    public boolean equals(Object obj){
        if(obj==null){
            return false;
        }
        if(this.id==null){
            //return (((Product)obj).getInvDescription().compareTo(this.invDescription)==0);


            if(((Product)obj).id==null){
                return (this == obj);
            } else {
                return false;
            }


        } else {
            if(((Product)obj).id!=null){
                return ((Product) obj).id.compareTo(this.id) == 0;
            } else {
                return false;
            }
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setInvDescription (String description){
        this.invDescription = description;
    }

    public String getInvDescription() {
        return invDescription;
    }

    public List<StoreBasedAttributes> getStoreBasedAttributes(){
        return new ArrayList<>();
    }

}
