
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "block",
        xCord: 0,
        yCord: 0
    }
    worker A {
        "text" -> B;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 32,
        yCord: 54
    }
    worker B {
        string txt = <- A;

        // This shouldn't be captured in name.
        12 -> function;

        // This should be captured in name.
        txt -> C;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 12,
        yCord: 4
    }
    worker C {
        string txt = <- B;
    }

    int x = <- B;
}
