package com.example.pdfreader.Entities;

import com.example.pdfreader.Helpers.SupplierProductRelation;
import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.REMOVE)
    private Set<SupplierProductRelation> supplierProductRelations;

    public Supplier(){}
    public Supplier(String name){
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
