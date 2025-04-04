import ballerina/lang.value;

// Convert MenuItem to a JSON representation
function menuItemToJson(MenuItem item) returns json => {
    itemCode: item.itemCode,
    itemName: item.itemName,
    price: item.price.toString()
};

// Convert OrderItem to a JSON representation
function orderItemToJson(OrderItem item) returns json => {
    itemCode: item.itemCode,
    quantity: item.quantity
};

// Convert from JSON to Order
function toOrder(json jsonData) returns Order|error => value:fromJsonWithType(jsonData);


// Calculate total for an order based on order items and menu items
function calculateOrderTotal(OrderItem[] items) returns decimal => <decimal>items.length();

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
