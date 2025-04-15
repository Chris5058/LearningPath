package com.example.elasticsearchlearner.controller;
import com.example.elasticsearchlearner.Person;
import com.example.elasticsearchlearner.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/people")
public class PersonController {
    @Autowired
    private PersonRepository repository;

    @PostMapping
    public Person addPerson(@RequestBody Person person) {
        return repository.save(person);
    }

    @GetMapping("/{id}")
    public Person getPerson(@PathVariable String id) {
        return repository.findById(id).orElse(null);
    }
}