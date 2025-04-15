package com.example.elasticsearchlearner.repository;
import com.example.elasticsearchlearner.Person;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
public interface PersonRepository extends ElasticsearchRepository<Person, String>{



}
