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
        templateId: "TransformNode",
        xCord: 100,
        yCord: 24
    }
    worker DataMapper {
        Student s = transform({
            firstName: "John",
            lastName: "Doe"
        });
        s -> function;
    }

    Student s = <- DataMapper;
}

function transform(Person p) returns Student => {
    fullName: p.firstName + " " + p.lastName
};
