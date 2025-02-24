import ballerina/graphql;

type Profile distinct service object {
    resource function get name() returns string;
};

distinct service class Teacher {
    *Profile;

    private string name;
    private final string subject;

    function init(string name, string subject) {
        self.name = name;
        self.subject = subject;
    }

    resource function get name() returns string {
        return self.name;
    }

    resource function get subject() returns string {
        return self.subject;
    }

    resource function get profile() returns Profile {
        return self;
    }
}

distinct service class Student {
    *Profile;

    private string name;

    function init(string name) {
        self.name = name;
    }

    resource function get name() returns string {
        return "Jesse Pinkman";
    }
}

service /graphql/api on new graphql:Listener(9090) {

    resource function get profiles() returns Profile[] {
        return [new Teacher("Walter White", "Chemistry"), new Student("Jesse Pinkman")];
    }
}
