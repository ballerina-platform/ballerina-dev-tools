import ballerina/http;
import ballerina/io;

enum DanceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

const float REGISTRATION_FEE = 50.0;

int totalStudents = 0;

type Student record {
    string name;
    int age;
    DanceLevel level;
};

type DanceClass object {
    string className;
    Student[] students;

    function addStudent(Student student);
    function removeStudent(string name);
};

class BalletClass {
    *DanceClass;

    function init(string name) {
        self.className = name;
        self.students = [];
    }

    function addStudent(Student student) {
        self.students.push(student);
        totalStudents += 1;
    }

    function removeStudent(string name) {
        int? index = self.students.indexOf(self.students.filter(s => s.name == name)[0]);
        if (index is int) {
            _ = self.students.remove(index);
            totalStudents -= 1;
        }
    }
}

type DanceSchool record {
    string name;
    DanceClass[] classes;
};

function calculateTuition(Student student) returns float {
    float baseTuition = 100.0;
    match student.level {
        BEGINNER => { return baseTuition; }
        INTERMEDIATE => { return baseTuition * 1.2; }
        ADVANCED => { return baseTuition * 1.5; }
        _ => { return baseTuition; } 
    }
}

service /danceschool on new http:Listener(8080) {

    resource function get students() returns json[] {
        DanceSchool school = {
            name: "Graceful Steps Academy",
            classes: []
        };
        
        BalletClass beginnerBallet = new ("Beginner Ballet");
        beginnerBallet.addStudent({name: "Alice", age: 10, level: BEGINNER});
        beginnerBallet.addStudent({name: "Bob", age: 12, level: BEGINNER});
        
        school.classes.push(beginnerBallet);
        
        return school.classes[0].students.map(student =>
            {
                name: student.name,
                age: student.age,
                level: student.level,
                tuition: calculateTuition(student)
            }
        );
    }
}

listener http:Listener httpListener = new (8080);

public function main() {
    io:println("Dance School Management System Started!");
    io:println("Total Registration Fee: " + (totalStudents * REGISTRATION_FEE).toString());
}
