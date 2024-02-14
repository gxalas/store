package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.stream.Collectors;

public class ProductDetailsDTO {
    private Long productId;
    private String productCode; // Assuming there's a code field in Product you're interested in.
    private String productInvDescription; // Assuming you want to include the product's description.
    private String productInvMaster;
    private List<StoreBasedAttributes> storeBasedAttributes = new ArrayList<>();
    private List<String> supplierNames = new ArrayList<>(); // Assuming you're only interested in supplier names.

    public ProductDetailsDTO(Long productId, String productCode, String productInvDescription,String productInvMaster) {
        this.productId = productId;
        this.productCode = productCode;
        this.productInvDescription = productInvDescription;
        this.productInvMaster = productInvMaster;
    }
    public ProductDetailsDTO(){}
    public ProductDetailsDTO(Product product,List<StoreBasedAttributes> sbas, List<String> supplierNames) {
        this.productId = product.getId();
        this.productCode = product.getCode();
        this.productInvDescription = product.getInvDescription();
        this.productInvMaster = product.getInvmaster();
        this.storeBasedAttributes = sbas;
        this.supplierNames = supplierNames;
    }

    public Long getId() {
        return productId;
    }

    public String getCode() {
        return productCode;
    }

    public String getInvDescription() {
        return productInvDescription;
    }
    public void setInvDescription(String productDescription) {
        this.productInvDescription = productDescription;
    }

    public String getProductInvMaster() {
        return productInvMaster;
    }

    public void setProductInvMaster(String productInvMaster) {
        this.productInvMaster = productInvMaster;
    }

    public void setId(Long productId) {
        this.productId = productId;
    }

    public List<StoreBasedAttributes> getStoreBasedAttributes() {
        return storeBasedAttributes;
    }

    public void setStoreBasedAttributes(List<StoreBasedAttributes> storeBasedAttributes) {
        this.storeBasedAttributes = storeBasedAttributes;
    }

    public List<String> getSupplierNames() {
        return supplierNames;
    }



    public void setSupplierNames(List<String> supplierNames) {
        this.supplierNames = supplierNames;
    }

    public void setCode(String productCode) {
        this.productCode = productCode;
    }

    public static List<ProductDetailsDTO> fetchAllProductDetails(EntityManager entityManager) {
        // Fetch Products
        List<Product> products = entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        // Fetch StoreBasedAttributes for each Product
        List<StoreBasedAttributes> sbas = entityManager.createQuery("SELECT sba FROM StoreBasedAttributes sba", StoreBasedAttributes.class).getResultList();
        Map<Long, List<StoreBasedAttributes>> sbaMap = sbas.stream()
                .filter(sba -> sba.getProduct() != null) // Add this filter
                .collect(Collectors.groupingBy(sba -> sba.getProduct().getId()));

        // Fetch Suppliers and map them by Product ID
        List<Object[]> supplierData = entityManager.createQuery("SELECT spr.product.id, s.name FROM SupplierProductRelation spr JOIN spr.supplier s", Object[].class).getResultList();
        Map<Long, List<String>> supplierMap = new HashMap<>();
        for (Object[] row : supplierData) {
            Long productId = (Long) row[0];
            String supplierName = (String) row[1];
            supplierMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(supplierName);
        }

        // Assemble DTOs
        List<ProductDetailsDTO> productDetailsDTOs = new ArrayList<>();
        for (Product product : products) {
            ProductDetailsDTO dto = new ProductDetailsDTO();
            dto.setId(product.getId());
            dto.setCode(product.getCode());
            dto.setInvDescription(product.getInvDescription());
            dto.setProductInvMaster(product.getInvmaster());
            dto.setStoreBasedAttributes(sbaMap.getOrDefault(product.getId(), Collections.emptyList()));
            dto.setSupplierNames(supplierMap.getOrDefault(product.getId(), Collections.emptyList()));
            productDetailsDTOs.add(dto);
        }

        return productDetailsDTOs;
    }

    public static List<ProductDetailsDTO> fetchAllProductDetailsWithSuppliers(EntityManager entityManager) {
        // Fetch all products
        List<Product> products = entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        // Prepare DTOs with basic product details
        Map<Long, ProductDetailsDTO> dtos = products.stream().collect(Collectors.toMap(
                Product::getId,
                p -> new ProductDetailsDTO(p.getId(), p.getCode(), p.getInvDescription(), p.getInvmaster())
        ));

        // Fetch StoreBasedAttributes and map to products, handling null product references
        List<StoreBasedAttributes> attributes = entityManager.createQuery("SELECT spa FROM StoreBasedAttributes spa", StoreBasedAttributes.class).getResultList();
        attributes.forEach(spa -> {
            if (spa.getProduct() != null) { // Check if there's a product reference
                ProductDetailsDTO dto = dtos.get(spa.getProduct().getId());
                if (dto != null) {
                    // Assuming you have a way to represent StoreBasedAttributes in your DTO
                    // Adjust this part based on your actual data structure for StoreBasedAttributes in the DTO
                    dto.getStoreBasedAttributes().add(spa); // This line needs to be adjusted to your DTO's structure
                }
            }
        });

        // Fetch supplier names and map to products
        List<Object[]> supplierMappings = entityManager.createQuery(
                "SELECT spr.product.id, s.name FROM SupplierProductRelation spr JOIN spr.supplier s", Object[].class
        ).getResultList();

        supplierMappings.forEach(mapping -> {
            Long productId = (Long) mapping[0];
            String supplierName = (String) mapping[1];
            ProductDetailsDTO dto = dtos.get(productId);
            if (dto != null) {
                dto.getSupplierNames().add(supplierName);
            }
        });

        return new ArrayList<>(dtos.values());
    }
    // Getters and setters
}
