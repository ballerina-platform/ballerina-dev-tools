public type Student record {
    string fullName;
};

public type Person record {
    string firstName;
    string lastName;
};

@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 0,
        yCord: 0
    }
    worker A {
        Person p = {
            firstName: "John",
            lastName: "Doe"
        };

        p -> B;
    }

    @display {
        label: "Node",
        templateId: "TransformNode",
        xCord: 100,
        yCord: 24
    }
    worker B returns error? {
        Person p = <- A;
        Student s = check transform(p);
        s -> function;
    }

    Student|error s = <- B;
}

function transform(Person p) returns Student|error => {
    fullName: p.firstName + " " + p.lastName
};
