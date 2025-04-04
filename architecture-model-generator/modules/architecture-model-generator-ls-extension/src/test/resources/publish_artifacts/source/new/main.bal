import ballerina/http;

listener http:Listener httpListener = new (port);

// In-memory tables to store menu items and orders
table<MenuItem> key(itemCode) menuItems = table [
    {itemCode: "RICE001", itemName: "Fried Rice", price: 350.00},
    {itemCode: "NOOD001", itemName: "Noodles", price: 300.00},
    {itemCode: "PIZZA001", itemName: "Pizza", price: 1200.00},
    {itemCode: "BURGER001", itemName: "Burger", price: 500.00}
];

table<Order> key(orderId) orders = table [
    {
        orderId: "ORD001",
        items: [
            {itemCode: "RICE001", quantity: 2},
            {itemCode: "NOOD001", quantity: 1}
        ],
        total: 1000.00,
        status: "PENDING"
    },
    {
        orderId: "ORD002",
        items: [
            {itemCode: "PIZZA001", quantity: 1},
            {itemCode: "BURGER001", quantity: 2}
        ],
        total: 2200.00,
        status: "COMPLETED"
    }
];

// Order counter for generating order IDs
int orderCounter = 0;

service /restaurant on httpListener {

    resource function get items() returns MenuItem[]|error {
        MenuItem[] menuItemList = [];
        foreach MenuItem item in menuItems {
            menuItemList.push(item);
        }
        return menuItemList;
    }

    resource function post orders(@http:Payload OrderRequest orderRequest) returns Order|error {
        orderCounter += 1;
        string newOrderId = "ORD" + orderCounter.toString();

        decimal orderTotal = 0;
        foreach OrderItem orderItem in orderRequest.items {
            MenuItem? menuItem = menuItems[orderItem.itemCode];
            if menuItem is () {
                return error("Invalid item code: " + orderItem.itemCode);
            }
            orderTotal += menuItem.price * orderItem.quantity;
        }

        Order newOrder = {
            orderId: newOrderId,
            items: orderRequest.items,
            total: orderTotal,
            status: "PENDING"
        };

        orders.add(newOrder);
        return newOrder;
    }
}
