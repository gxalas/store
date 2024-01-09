package com.example.pdfreader.Entities.Attributes;


import com.example.pdfreader.enums.StoreNames;
import jakarta.persistence.*;


/*
Product Class attribute
 */
@Entity
@Table(name = "store_based_attributes")
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
    // Getters and setters...
}

