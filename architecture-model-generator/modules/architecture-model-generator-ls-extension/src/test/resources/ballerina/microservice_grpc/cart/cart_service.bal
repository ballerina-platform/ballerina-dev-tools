import ballerina/grpc;

configurable string datastore = "";
configurable string redisHost = "";
configurable string redisPassword = "";
listener grpc:Listener ep = new (9092);

@display {
    label: "Cart",
    id: "cart"
}
@grpc:ServiceDescriptor {descriptor: ROOT_DESCRIPTOR_DEMO, descMap: getDescriptorMapDemo()}
service "CartService" on ep {
    @display {
        label: "Data Store",
        id: "dataStore"
    }
    private final DataStore store;

    function init() returns error? {
        if datastore == "redis" {
            self.store = check new RedisStore();
        } else {
            self.store = new InMemoryStore();
        }
    }

    remote function AddItem(AddItemRequest value) returns Empty|error {
        lock {
            error? result = self.store.add(value.user_id, value.item.product_id, value.item.quantity);
            if result is error {
                return result;
            }
        }
        return {};
    }
    remote function GetCart(GetCartRequest value) returns Cart|error {
        lock {
            Cart cart = check self.store.getCart(value.user_id);
            return cart.cloneReadOnly();
        }
    }
    remote function EmptyCart(EmptyCartRequest value) returns Empty|error {
        lock {
            error? result = self.store.emptyCart(value.user_id);
            if result is error {
                return result;
            }
        }
        return {};
    }
}
