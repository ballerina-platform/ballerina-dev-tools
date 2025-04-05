import ballerina/lang.value;

// Convert MenuItem to a JSON representation
function menuItemToJson(MenuItem item) returns json => {
    itemCode: item.itemCode,
    itemName: item.itemName,
    price: item.price.toString()
};

// Convert from JSON to MenuItem
function jsonToMenuItem(json jsonData) returns MenuItem|error => value:fromJsonWithType(jsonData);

// Convert OrderItem to a JSON representation
function orderItemToJson(OrderItem item) returns json => {
    itemCode: item.itemCode,
    quantity: item.quantity
};

// Convert from JSON to OrderItem
function jsonToOrderItem(json jsonData) returns OrderItem|error => value:fromJsonWithType(jsonData);

// Convert from JSON to Order
function jsonToOrder(json jsonData) returns Order|error => value:fromJsonWithType(jsonData);

// Transform OrderRequest to Order with a new ID
function orderRequestToOrder(string orderId, OrderRequest request) returns Order => {
    orderId: orderId,
    items: request.items,
    total: calculateOrderTotal(request.items),
    status: "PENDING"
};

// Calculate total for an order based on order items and menu items
function calculateOrderTotal(OrderItem[] items) returns decimal {
    // This is a placeholder - in a real application, you would look up
    // each item in a database or service to get its price
    return 0.0;
}

// Map from one version of MenuItem to another (example of type transformation)
function mapMenuItemV1ToV2(MenuItem itemV1) returns record {|
    string code;
    string name;
    decimal unitPrice;
|} => {
    code: itemV1.itemCode,
    name: itemV1.itemName,
    unitPrice: itemV1.price
};
