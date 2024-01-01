package com.example.pdfreader.Controllers.States;

import com.example.pdfreader.DTOs.ProductDTO;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.enums.StoreNames;

public record ProductViewState(String searchTxt, StoreNames selectedStore, ProductDTO productDto) {
}
