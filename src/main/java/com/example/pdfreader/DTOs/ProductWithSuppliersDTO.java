package com.example.pdfreader.DTOs;

import com.example.pdfreader.DAOs.ProductDAO;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Entities.Supplier;

import java.util.ArrayList;
import java.util.List;

public class ProductWithSuppliersDTO {
    private Product product;
    private List<Supplier> suppliers = new ArrayList<>();

    // Constructor
    public ProductWithSuppliersDTO(Product product) {
        this.product = product;
    }

    // Getters and setters
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    public void addSupplier(Supplier supplier) {
        this.suppliers.add(supplier);
    }
    /*
    wrong
    */
    public static List<ProductWithSuppliersDTO> getAllProductsWithSuppliers() {
        List<ProductWithSuppliersDTO> dtos = new ArrayList<>();
        // Assuming you have a method in your DAO to fetch all products
        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.getAllProducts(); // Implement this method to fetch all products

        return dtos;
    }
}