package com.example.elasticsearchlearner.controller;


import com.example.elasticsearchlearner.model.Product;
import com.example.elasticsearchlearner.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductRepository repository;

    // Add a product (real-time indexing)
    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return repository.save(product);
    }

    // Search products by name (real-time search)
    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return repository.findByNameContaining(name);
    }

    // Get a product by ID
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable String id) {
        return repository.findById(id).orElse(null);
    }
}
