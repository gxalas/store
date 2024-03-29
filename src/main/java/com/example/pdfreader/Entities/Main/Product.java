package com.example.pdfreader.Entities.Main;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import jakarta.persistence.*;

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
     * this will be moved
     * to the store based
     * attributes
     */
    @Column(name = "description")
    private String invDescription;

    @Column(name = "invmaster")
    private String invmaster;

    @Column(name = "log")
    private String log;


    public Product(String productCode,String description,String master){
        this.code = productCode;
        this.invDescription = description;
        this.invmaster = master;
    }


    public Product(){
    }

    public String getInvmaster(){
        return this.invmaster;
    }

    public String getCode(){
        if(this.code!=null){
            return this.code;
        }
        return "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Product)){
           return false;
        }
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

    public void setInvmaster(String m){
        this.invmaster = m;
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

    public String getLog() {
        return log;
    }
    public void setLog(String log) {
        this.log = log;
    }
}
