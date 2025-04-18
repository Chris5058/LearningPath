package com.example.elasticsearchlearner;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "people")
public class Person {
    @Id
    private String id;
    private String name;
    private int age;

    // Getters, setters, and constructors
    public Person() {}
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}