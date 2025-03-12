package com.example.springexceptionhandling.service;

import com.example.springexceptionhandling.model.Book;
import com.example.springexceptionhandling.problemDetailConcepts.BookNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookServiceImpl {
    //implementation of the service
    public final Map<Long, Book> books = new HashMap<>();

    public BookServiceImpl() {
        books.put(1L, new Book("Harry Potter", "J.K. Rowling", "1234"));
        books.put(2L, new Book("The Hobbit", "J.R.R. Tolkien", "5678"));
    }

    public Book getBookById(Long id) {
        Book book = books.get(id);
        if (book == null) {
            throw new BookNotFoundException(id);
        }
        return book;
    }

    public Book addBook(Book book) {
        books.put(book.getId(), book);
        return book;
    }

}
