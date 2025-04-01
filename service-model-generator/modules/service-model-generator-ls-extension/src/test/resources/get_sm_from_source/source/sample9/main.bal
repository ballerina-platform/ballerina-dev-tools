import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

service PizzaShop on httpListener {

    resource function get pizzas() returns Pizza[] {
        do {
        } on fail error e {
            return [];
        }
    }

    resource function get orders(string? customerId) returns Order[] {
        do {
        } on fail error e {
            return [];
        }
    }

    resource function post orders(OrderRequest payload) returns Order|http:BadRequest {
        do {
        } on fail error e {
            return http:BAD_REQUEST;
        }
    }

    resource function get orders/[string orderId]() returns Order|http:NotFound {
        do {
        } on fail error e {
            return http:NOT_FOUND;
        }
    }

    resource function patch orders/[string orderId](OrderUpdate payload) returns http:Ok|http:BadRequest {
        do {
        } on fail error e {
            return http:BAD_REQUEST;
        }
    }
}
