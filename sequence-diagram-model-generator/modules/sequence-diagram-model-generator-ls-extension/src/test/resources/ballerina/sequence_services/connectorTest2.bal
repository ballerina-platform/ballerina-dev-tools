import ballerina/http;

service on new http:Listener(0) {
    // endpoint declaration
    http:Client lclHttpEp;
    // endpoint declaration with access modifier
    private http:Client pvtHttpEp;
    // endpoint declaration with display annotation
    @display {
        label: "Http endpoint with display annotation"
    }
    http:Client antHttpEp;
    // endpoint declaration with access modifier and display annotation
    @display {
        label: "Http endpoint with display annotation and access modifier"
    }
    public http:Client publHttpEp;

    function init() returns error? {
        self.lclHttpEp = check new (url = "");
        self.pvtHttpEp = check new (url = "");
        self.antHttpEp = check new (url = "");
        self.publHttpEp = check new (url = "");
    }

    resource function get path() returns error? {
        json get = check self.lclHttpEp->get("/users");
        json pvtGet = check self.pvtHttpEp->get("/users");
        json anttGet = check self.antHttpEp->get("/users");
        json pblcGet = check self.publHttpEp->get("/users");
    }
}
