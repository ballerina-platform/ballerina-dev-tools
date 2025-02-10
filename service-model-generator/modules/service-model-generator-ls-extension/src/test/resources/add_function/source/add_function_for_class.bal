public service class Book {
    private final string id;
    private final string name;
    private Author[] authors = [];

}

type Author record {|
    int id;
    string name;
|};
