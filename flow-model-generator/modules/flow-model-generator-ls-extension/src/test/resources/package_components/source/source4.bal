import ballerina/http;
import ballerina/log;
import ballerinax/trigger.salesforce as sfdc;

listener http:Listener httpListener = new (8090);
configurable sfdc:ListenerConfig configuration = {
    username: "USER_NAME",
    password: "PASSWORD" + "SECURITY_TOKEN",
    channelName: "CHANNEL_NAME"
};
listener sfdc:Listener sfdcListener = new (configuration);

service sfdc:RecordService on sfdcListener {
    remote function onUpdate(sfdc:EventData event) returns error? {
        log:printInfo(event.toString());
    }

    remote function onCreate(sfdc:EventData event) returns error? {

    }

    remote function onDelete(sfdc:EventData event) returns error? {

    }

    remote function onRestore(sfdc:EventData event) returns error? {

    }

    function nonRemoteFunction() {

    }
}
