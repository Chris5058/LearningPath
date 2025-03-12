package com.example.springexceptionhandling.model;

public class Book {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private Long id;
    private String title;
    private String author;

    public Book(String title, String author, String isbn) {
        this.title = title;
        this.author = author;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

}
