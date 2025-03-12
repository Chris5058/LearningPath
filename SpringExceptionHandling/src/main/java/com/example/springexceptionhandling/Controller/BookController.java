package com.example.springexceptionhandling.Controller;

import com.example.springexceptionhandling.model.Book;
import com.example.springexceptionhandling.service.BookServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookServiceImpl bookService;

    public BookController(BookServiceImpl bookService) {
        this.bookService = bookService;
    }
    @GetMapping("/{id}")
    public Book getBook(@PathVariable Long id) {
        return bookService.getBookById(id);
    }
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.addBook(book);
    }
}
