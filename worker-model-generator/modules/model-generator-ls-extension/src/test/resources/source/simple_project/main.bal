
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 32,
        yCord: 54
    }
    worker A {
        Request req = {id: "1", sender: "A", value: 10};

        req -> B;
    }

    @display {
        label: "Node",
        templateId: "SwitchNode",
        xCord: 0,
        yCord: 132
    }
    worker B {
        Request req = <- A;

        if (req.value > 5) {
            req -> C;
        } else {
            req -> D;
        }
    }

    @display {
        label: "Node",
        templateId: "CodeBlockNode",
        xCord: 10,
        yCord: 12
    }
    worker C {
        Request req = <- B;

        Response res = {id: req.id, success: true};
        res -> function;
    }

    @display {
        label: "Node",
        templateId: "CodeBlockNode",
        xCord: 132,
        yCord: 24
    }
    worker D {
        Request req = <- B;

        Response res = {id: req.id, success: false};
        res -> function;
    }

    Response res1 = <- C;
    Response res2 = <- D;
}
