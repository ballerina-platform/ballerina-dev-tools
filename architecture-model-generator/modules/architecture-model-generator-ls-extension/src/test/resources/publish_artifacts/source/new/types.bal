type MenuItem record {|
    readonly string itemCode;
    string itemName;
    decimal price;
|};

type OrderItem record {|
    string itemCode;
    int quantity;
|};

type Order record {|
    readonly string orderId;
    OrderItem[] items;
    decimal total;
    string status;
|};

type OrderRequest record {|
    OrderItem[] items;
|};
