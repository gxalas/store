package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.Main.Product;

public class ProductWithSupplierCount {
    private final Product product;
    private Integer supplierCount;

    public ProductWithSupplierCount(Product product, Long supplierCount) {
        this.product = product;
        this.supplierCount = Math.toIntExact(supplierCount);
    }

    // Getters
    public Product getProduct() {
        return product;
    }

    public int getSupplierCount() {
        return supplierCount;
    }

    public void setSupplierCount(int value){
        this.supplierCount = value;
    }

}