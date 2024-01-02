import simple_project.types;

@display {
    label: "Flow",
    id: "5",
    name: "simple_import/function"
}
public function testSimpleImports() {

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 0,
        yCord: 0
    }
    worker A {
        types:Request req = {
            name: "A",
            address: "1/1/address",
            age: 11
        };

        req -> C;
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 0,
        yCord: 10
    }
    worker B {
        Request req = {
            sender: "foo",
            id: "1",
            value: 32
        };
        req -> C;
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 0,
        yCord: 20
    }
    worker C {
        types:Request writeConfig = <- A;
        Request writeConfig2 = <- B;
    }
}
