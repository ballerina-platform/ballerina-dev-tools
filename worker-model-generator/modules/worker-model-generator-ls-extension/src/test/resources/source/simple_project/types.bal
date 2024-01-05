type Request record {
    string id;
    string sender;
    int value;
};

type Response record {|
    string id;
    boolean success;
|};
