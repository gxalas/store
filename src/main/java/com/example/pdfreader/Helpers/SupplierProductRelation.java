package com.example.pdfreader.Helpers;

import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.Supplier;
import jakarta.persistence.*;

@Entity
public class SupplierProductRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "supplier_id", referencedColumnName = "id")
    private Supplier supplier;

    public SupplierProductRelation(){}
    public SupplierProductRelation(Product product, Supplier supplier){
        this.product = product;
        this.supplier = supplier;
    }

    public Long getId() {
        return id;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
}
