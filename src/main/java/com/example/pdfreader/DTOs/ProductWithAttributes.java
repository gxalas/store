package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.enums.StoreNames;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.*;

public class ProductWithAttributes {
    private Product product;
    private Map<StoreNames, StoreBasedAttributes> attributes;

    public Product getProduct(){
        return this.product;
    }
    public Map<StoreNames,StoreBasedAttributes> getAttributes(){
        return this.attributes;
    }

    // Constructors, getters, and setters

    public ProductWithAttributes(Product product) {
        this.product = product;
        this.attributes = new HashMap<>();
    }
    public ProductWithAttributes(Product product,StoreBasedAttributes sba) {
        this.product = product;
        this.attributes = new HashMap<>();
        this.attributes.put(sba.getStore(),sba);
    }

    public void addStoreBasedAttribute(StoreNames store, StoreBasedAttributes storeAttribute) {
        this.attributes.put(store, storeAttribute);
    }

    // Static method to get all products with their StoreBasedAttributes
    public static List<ProductWithAttributes> getAllProductsWithAttributes(EntityManager entityManager) {
        TypedQuery<StoreBasedAttributes> query = entityManager.createQuery(
                "SELECT sba FROM StoreBasedAttributes sba", StoreBasedAttributes.class);
        List<StoreBasedAttributes> storeBasedAttributesList = query.getResultList();

        Map<Long, ProductWithAttributes> productMap = new HashMap<>();
        for (StoreBasedAttributes sba : storeBasedAttributesList) {
            Product product = sba.getProduct(); // Get the associated Product

            if (product != null) {
                ProductWithAttributes productWithAttributes = productMap.computeIfAbsent(
                        product.getId(), id -> new ProductWithAttributes(product));
                productWithAttributes.addStoreBasedAttribute(sba.getStore(), sba);
            }
        }

        return new ArrayList<>(productMap.values());
    }

    public static Map<String,ProductWithAttributes> getMapOfProductsWithAttributes(EntityManager entityManager) {
        TypedQuery<StoreBasedAttributes> query = entityManager.createQuery(
                "SELECT sba FROM StoreBasedAttributes sba", StoreBasedAttributes.class);
        List<StoreBasedAttributes> storeBasedAttributesList = query.getResultList();

        Map<Long, ProductWithAttributes> productMap = new HashMap<>();
        for (StoreBasedAttributes sba : storeBasedAttributesList) {
            Product product = sba.getProduct(); // Get the associated Product

            if (product != null) {
                ProductWithAttributes productWithAttributes = productMap.computeIfAbsent(
                        product.getId(), id -> new ProductWithAttributes(product));
                productWithAttributes.addStoreBasedAttribute(sba.getStore(), sba);
            }
        }
        List<ProductWithAttributes> result = new ArrayList<>(productMap.values());
        Map<String,ProductWithAttributes> map = new HashMap<>();
        result.forEach(productWithAttributes -> {
            Set<String> productBarcodes = new HashSet<>();
            productWithAttributes.attributes.values().forEach(sba->{
                productBarcodes.addAll(sba.getBarcodes());
            });
            productBarcodes.forEach(barcode->{
                if(map.get(barcode)!=null){
                    System.out.println("possible conflict with the barcode : "+barcode);
                } else {
                    map.put(barcode,productWithAttributes);
                }
            });
        });
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        return ((ProductWithAttributes)obj).getProduct().equals(this.product);
        //return super.equals(obj);
    }
}
