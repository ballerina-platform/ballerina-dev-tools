import ballerina/http;
import ballerinax/trigger.slack;

configurable slack:ListenerConfig config = ?;

listener http:Listener httpListener = new (8090);
listener slack:Listener webhookListener = new (config, httpListener);

service slack:AppService on webhookListener {

    remote function onAppMention(slack:GenericEventWrapper payload) returns error? {
        do {
            //Not Implemented
        } on fail error e {
            return e;
        }
    }

    remote function onAppRateLimited(slack:GenericEventWrapper payload) returns error? {
        do {
            //Not Implemented
        } on fail error e {
            return e;
        }
    }

    remote function onAppUninstalled(slack:GenericEventWrapper payload) returns error? {
        do {
            //Not Implemented
        } on fail error e {
            return e;
        }
    }
}

service slack:DndService on webhookListener {

    remote function onDndUpdated(slack:GenericEventWrapper payload) returns error? {
        do {
            //Not Implemented
        } on fail error e {
            return e;
        }
    }

    remote function onDndUpdatedUser(slack:GenericEventWrapper payload) returns error? {
        do {
            //Not Implemented
        } on fail error e {
            return e;
        }
    }

    function nonRemoteFunction() {

    }
}
