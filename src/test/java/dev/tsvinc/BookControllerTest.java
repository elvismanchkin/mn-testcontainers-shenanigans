package dev.tsvinc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.datafaker.Faker;

@MicronautTest(environments = "test")
@Testcontainers
public class BookControllerTest {
    private static final Faker faker = new Faker();

    // in case your registry requires auth
    /*@BeforeClass
    public static void setup() {
        RegistryAuthLocator.instance().addAuthentication("your-private-registry.example.com", "username", "password");
    }*/

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    BookRepository bookRepository;

    @Test
    void testSaveAndRetrieveBook() {
        var title = faker.book().title();
        var isbn = faker.code().isbn13();
        var bookToSave = new Book(title, isbn);
        HttpRequest<Book> request = HttpRequest.POST("/books", bookToSave);
        var response = client.toBlocking().exchange(request, Book.class);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertNotNull(response.body());
        assertNotNull(response.body().getId());
        assertEquals(title, response.body().getTitle());
        assertEquals(isbn, response.body().getIsbn());

        var savedId = response.body().getId();
        var location = response.getHeaders().get("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/books/" + savedId));

        HttpRequest<Book> retrieveRequest = HttpRequest.GET("/books/" + savedId);
        var retrievedBook = client.toBlocking().retrieve(retrieveRequest, Book.class);

        assertNotNull(retrievedBook);
        assertEquals(savedId, retrievedBook.getId());
        assertEquals(title, retrievedBook.getTitle());

        HttpRequest<Book> retrieveByIsbnRequest = HttpRequest.GET("/books/isbn/" + bookToSave.getIsbn());
        var retrievedByIsbn = client.toBlocking().retrieve(retrieveByIsbnRequest, Book.class);
        assertNotNull(retrievedByIsbn);
        assertEquals(savedId, retrievedByIsbn.getId());
        assertEquals(bookToSave.getIsbn(), retrievedByIsbn.getIsbn());
    }

    @Test
    void testListBooks() {
        var title = faker.book().title();
        var isbn = faker.code().isbn13();
        var book1 = new Book(title, isbn);
        client.toBlocking().exchange(HttpRequest.POST("/books", book1));

        var title2 = faker.book().title();
        var isbn2 = faker.code().isbn13();
        var book2 = new Book(title2, isbn2);
        client.toBlocking().exchange(HttpRequest.POST("/books", book2));

        HttpRequest<Iterable<Book>> listRequest = HttpRequest.GET("/books");
        Iterable<Book> books = client.toBlocking().retrieve(listRequest, Argument.listOf(Book.class));

        assertNotNull(books);
        var count = StreamSupport.stream(books.spliterator(), false).count();
        assertTrue(count >= 2);
    }

    @Test
    void testRetrieveNonExistentBook() {
        HttpRequest<Book> retrieveRequest = HttpRequest.GET("/books/%d".formatted(bookRepository.findMaxId() + 1));

        var thrown = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(retrieveRequest, Book.class),
                "Expected HttpClientResponseException for non-existent ID");
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }
}
