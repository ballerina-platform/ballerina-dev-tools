import ballerina/http;

http:Client moduleHttpCl = check new ("http://localhost:9090");
http:Client moduleHttpExCl = check new http:Client("http://localhost:9090");
