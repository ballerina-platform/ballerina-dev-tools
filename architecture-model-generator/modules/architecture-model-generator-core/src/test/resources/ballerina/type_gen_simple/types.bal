import ballerina/constraint;

public type User record {
    readonly string userId;
    string name;
    string contactNo;
    string email;
    Profile profile;
};

public type Kid record {
    *User;
    string level;
    Adult guardian;
};

public type Adult record {
    *User;
    string occupation;
    string officeContactNo?;
    string personalContactNo = "";
};

public type Profile record {
    readonly string id;
    string username;
    AnuualSub|MonthlySub subscription;
};

type AnuualSub record {
    int id;
    int 'limit;
    float fee;
};

type MonthlySub record {
    int id;
    float fee;
};

public type Address record {
    int no;
    string street;
};

public type Job record {|
    string jobName;
    string jobType?;
    string...;
|};

public type Person record {
    string personName;
    int age;
    record {
        int no;
        string street;
    } address;
    Job job?;
};

public type Student record {
    string name;
    record {
        int no;
        string street;
    } address?;
    record {
        string personName;
        int age;
        record {
            int no;
            string street;
        } address;
        Job job?;
    } parent;
    @constraint:Array {
        minLength: 1
    }
    record {
        int courseId;
        string courseName;
        record {
            string name;
        } lecturer;
        record {
            string name;
        }[] instructors;
    }[] courses;
};

public type ClassRoom record {
    string className;
    @constraint:Array {
        minLength: 1,
        maxLength: 40
    }
    Student[] students;
};

public type School record {
    string name;
    ClassRoom[] classRooms;
    @constraint:Array {
        maxLength: 15
    }
    Student[][] students;
    @constraint:Array {
        minLength: 7,
        maxLength: 26
    }
    record {
        string name;
    }[][] tutors;
};
