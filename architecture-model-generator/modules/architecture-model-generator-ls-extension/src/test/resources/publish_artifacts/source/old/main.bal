import ballerina/http;
import ballerinax/ai.agent;

listener agent:Listener telegramAgentListener = new (listenOn = check http:getDefaultListener());
listener http:Listener httpListener = new (port);

// In-memory tables to store menu items and orders
table<MenuItem> key(itemCode) menuItems = table [
    {itemCode: "RICE001", itemName: "Fried Rice", price: 350.00},
    {itemCode: "NOOD001", itemName: "Noodles", price: 300.00},
    {itemCode: "PIZZA001", itemName: "Pizza", price: 1200.00}
];

table<Order> key(orderId) orders = table [];

// Order counter for generating order IDs
int orderCounter = 0;

service /telegramAgent on telegramAgentListener {
    resource function post chat(@http:Payload agent:ChatReqMessage request) returns agent:ChatRespMessage|error {

        string stringResult = check _telegramAgentAgent->run(request.message);
        return {message: stringResult};
    }
}

service /restaurant on httpListener {
    resource function get menu() returns MenuItem[]|error {
        return menuItems.toArray();
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
