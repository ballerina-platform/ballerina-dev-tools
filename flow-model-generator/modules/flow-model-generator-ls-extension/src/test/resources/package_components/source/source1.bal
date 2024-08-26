import ballerina/http;
import ballerina/uuid;

// Define custom types
type ISBN string;

type Book record {
    ISBN isbn;
    string title;
    string author;
    int year;
};

type BookSummary record {
    string title;
    string author;
};

// Define a map to store books
map<Book> bookStore = {};

service /bookstore on new http:Listener(8080) {

    // Add a new book
    resource function post book(@http:Payload Book newBook) returns Book|error {
        ISBN isbn = uuid:createType1AsString();
        newBook.isbn = isbn;
        bookStore[isbn] = newBook;
        return newBook;
    }

    // Get a book by ISBN
    resource function get book/[ISBN isbn]() returns Book|http:NotFound {
        Book? book = bookStore[isbn];
        if book is () {
            return <http:NotFound>{body: "Book not found"};
        }
        return book;
    }

    // Update a book
    resource function put book/[ISBN isbn](@http:Payload Book updatedBook) returns Book|http:NotFound {
        if (bookStore.hasKey(isbn)) {
            bookStore[isbn] = updatedBook;
            return updatedBook;
        }
        return <http:NotFound>{body: "Book not found"};
    }

    // Delete a book
    resource function delete book/[ISBN isbn]() returns http:NoContent|http:NotFound {
        Book? removeIfHasKey = bookStore.removeIfHasKey(isbn);
        if (removeIfHasKey == ()) {
            return http:NO_CONTENT;
        }
        return <http:NotFound>{body: "Book not found"};
    }
}
