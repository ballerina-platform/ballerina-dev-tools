public service class Book {
    private final string id;
    private final string name;
    private Author[] authors = [];

    function init(string name) returns error? {
        do {
            self.id = "";
            self.name = name;
        } on fail error err {
            // hanlde error
        }
    }

    resource function get author/[int id]() returns string|error? {
        do {
            Author[] result = from Author author in self.authors
                where author.id == id
                limit 1
                select author;
            if result.length() > 0 {
                return result[0].name;
            }
            check error("Author not found!");
        } on fail error err {
            // hanlde error
            return err;
        }
    }

    remote function addAuthor(Author author) {
        do {
            self.authors.push(author);
        } on fail error err {
            // hanlde error
        }
    }
}

type Author record {|
    int id;
    string name;
|};
