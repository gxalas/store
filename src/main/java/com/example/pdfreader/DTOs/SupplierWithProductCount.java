package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.Main.Supplier;

public class SupplierWithProductCount {
    private final Supplier supplier;
    private Integer productCount;

    public SupplierWithProductCount(Supplier supplier, Integer productCount) {
        this.supplier = supplier;
        this.productCount = productCount;
    }

    // Getters and Setters
    public Supplier getSupplier() {
        return supplier;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }
}
