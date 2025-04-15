package com.example.elasticsearchlearner.repository;
import com.example.elasticsearchlearner.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByNameContaining(String name);
}