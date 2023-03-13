import ballerina/graphql;

service /fileUpload on new graphql:Listener(9090) {

    remote function fileUpload(graphql:Upload file) returns string|error {
        string fileName = file.fileName;

        return string `File ${fileName} successfully uploaded`;
    }

    resource function get getUploadedFileNames() returns string[] {
        return ["image1.png", "image2.png"];
    }
}
