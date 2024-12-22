import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

listener http:Listener httpsListener = new (port = 9091);

service AlbumsService on httpListener, httpsListener {

	resource function get albums () returns Album[] {
		do {
		} on fail error e {
			return [];
		}
	}

	resource function post albums (Album payload) returns Album|ErrorPayloadBadRequest {
		do {
		} on fail error e {
			return http:BAD_REQUEST;
		}
	}
}
