
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "clone",
        xCord: 0,
        yCord: 0
    }
    worker A {
        // This shouldn't be captured in name.
        "text" -> B;
    }

    @display {
        label: "Node",
        templateId: "clone",
        xCord: 32,
        yCord: 54
    }
    worker B {
        string txt = <- A;

        // This should be captured in name.
        txt -> C;
    }

    @display {
        label: "Node",
        templateId: "clone",
        xCord: 12,
        yCord: 4
    }
    worker C {
        string txt = <- B;
        txt -> function;
    }

    string y1 = <- C;
}
