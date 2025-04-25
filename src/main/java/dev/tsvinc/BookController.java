package dev.tsvinc;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Optional;

@Controller("/books")
public class BookController {
    @Inject
    private BookRepository bookRepository;

    @Get
    public Iterable<Book> list() {
        return bookRepository.findAll();
    }

    @Get("/{id}")
    public Optional<Book> get(Long id) {
        return bookRepository.findById(id);
    }

    @Post("/")
    public HttpResponse<Book> save(@Body Book book) {
        Book savedBook = bookRepository.save(book);
        return HttpResponse.created(savedBook).headers(headers -> headers.location(location(savedBook.getId())));
    }

    @Get("/isbn/{isbn}")
    public Optional<Book> findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    protected URI location(Long id) {
        return URI.create("/books/" + id);
    }
}
