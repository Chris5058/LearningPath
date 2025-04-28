package com.example.elasticsearchlearner.repository;
import com.example.elasticsearchlearner.Person;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends ElasticsearchRepository<Person, String>{

@Query
    ("""
            {
              "match": {
                "name": {
                  "query": "?0",
                  "fuzziness": "AUTO"
                }
              }
            }
            """)
    Iterable<Person> searchByName(String name);

}
