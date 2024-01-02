import ballerina/http;
import ballerina/lang.value;

final http:Client pineValleyEp = check new ("http://localhost:9091/pineValley/");
final http:Client grandOakEp = check new ("http://localhost:9092/grandOak/");

type PineValleyPayload record {
    string doctorType;
};

service / on new http:Listener(9090) {

    @display {
        label: "Flow",
        id: "1",
        name: "doctor/<type>"
    }
    resource function get doctor/[string doctorType]() returns json|error? {

        @display {
            label: "Check Doctor Type",
            templateId: "SwitchNode"
            xCord: 0,
            yCord: 12
        }
        worker switchDoctorType {
            if doctorType == "ENT" {
                () -> callGrandOak;
            } else {
                () -> callGrandOak;
                () -> buildPineValleyPayload;
            }
        }

        @display {
            label: "Build Pine Valley Payload",
            templateId: "CloneNode",
            xCord: 11,
            yCord: 25
        }
        worker buildPineValleyPayload {
            () _ = <- switchDoctorType;
            PineValleyPayload payload = {doctorType: doctorType};
            payload -> callPineValley;
        }

        @display {
            label: "Call Grand Oak",
            templateId: "HttpRequestNode",
            xCord: 25,
            yCord: 32
        }
        worker callPineValley returns error? {
            PineValleyPayload payload = <- buildPineValleyPayload;
            json res = check pineValleyEp->post("/doctors/", payload);
            res -> mergeResults;
        }

        @display {
            label: "Call Grand Oak",
            templateId: "HttpRequestNode",
            xCord: 55,
            yCord: 32
        }
        worker callGrandOak returns error? {
            () _ = <- switchDoctorType | switchDoctorType;
            json res = check grandOakEp->get("/doctor/" + doctorType);
            res -> mergeResults;
        }

        @display {
            label: "Merge Results",
            templateId: "TransformNode",
            xCord: 60,
            yCord: 80
        }
        worker mergeResults returns error? {
            json j1 = check <- callPineValley;
            json j2 = check <- callGrandOak;

            json res = check transformFunction(j1, j2);
            res -> respond;
        }

        @display {
            label: "Reply",
            templateId: "CloneNode",
            xCord: 88,
            yCord: 98
        }
        worker respond returns error? {
            json j = check <- mergeResults;
            j -> function;
        }

        json j = check <- respond;
        return j;
    }
}

function transformFunction(json j1, json j2) returns json|error => check value:mergeJson(j1, j2);
