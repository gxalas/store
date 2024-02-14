package com.example.pdfreader.DTOs;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.enums.StoreNames;
import jakarta.persistence.EntityManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductCompleteDTO {
    private Product product;
    private Map<StoreNames, StoreBasedAttributes> storeBasedAttributesMap = new HashMap<>();
    private List<String> supplierNames;

    public ProductCompleteDTO(Product product) {
        this.product = product;
    }

    // Getters and Setters
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Map<StoreNames, StoreBasedAttributes> getStoreBasedAttributesMap() {
        return storeBasedAttributesMap;
    }

    public void setStoreBasedAttributesMap(Map<StoreNames, StoreBasedAttributes> storeBasedAttributesMap) {
        this.storeBasedAttributesMap = storeBasedAttributesMap;
    }

    public List<String> getSupplierNames() {
        return supplierNames;
    }

    public void setSupplierNames(List<String> supplierNames) {
        this.supplierNames = supplierNames;
    }
    public static List<ProductCompleteDTO> fetchAllProductDetails(EntityManager entityManager) {
        // Fetch Products
        List<Product> products = entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        // Fetch StoreBasedAttributes for each Product and map them by Product ID
        List<StoreBasedAttributes> sbas = entityManager.createQuery("SELECT sba FROM StoreBasedAttributes sba", StoreBasedAttributes.class).getResultList();
        Map<Long, Map<StoreNames, StoreBasedAttributes>> sbaMap = sbas.stream()
                .filter(sba -> sba.getProduct() != null) // Ensure there's a product associated
                .collect(Collectors.groupingBy(
                        sba -> sba.getProduct().getId(),
                        Collectors.toMap(
                                StoreBasedAttributes::getStore,
                                sba -> sba,
                                (existing, replacement) -> existing) // In case of duplicates, keep the existing
                ));

        // Fetch Suppliers and map them by Product ID
        List<Object[]> supplierData = entityManager.createQuery(
                "SELECT spr.product.id, s.name FROM SupplierProductRelation spr JOIN spr.supplier s", Object[].class).getResultList();
        Map<Long, List<String>> supplierMap = supplierData.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        // Assemble DTOs
        List<ProductCompleteDTO> productCompleteDTOs = products.stream().map(product -> {
            ProductCompleteDTO dto = new ProductCompleteDTO(product);
            dto.setStoreBasedAttributesMap(sbaMap.getOrDefault(product.getId(), new HashMap<>()));
            dto.setSupplierNames(supplierMap.getOrDefault(product.getId(), Collections.emptyList()));
            return dto;
        }).collect(Collectors.toList());

        return productCompleteDTOs;
    }

    // Additional methods to manipulate the maps and lists can be added as necessary
}