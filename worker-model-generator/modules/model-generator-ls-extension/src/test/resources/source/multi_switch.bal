
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
        int x = 11;
        x -> B;
    }

    @display {
        label: "Node",
        templateId: "SwitchNode",
        xCord: 12,
        yCord: 3
    }
    worker B {
        int x = <- A;

        if (x < 10) {
            x -> C;
        } else if (x > 10 && x < 20) {
            x -> D;
        } else if (x > 20 && x < 40) {
            x -> E;
        } else {
            x -> F;
        }
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 10,
        yCord: 50
    }
    worker C {
        int x = <- B;
        x -> function;
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 12,
        yCord: 56
    }
    worker D {
        int x = <- B;
        x -> function;
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 13,
        yCord: 52
    }
    worker E {
        int y = <- B;
        y -> function;
    }

    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 18,
        yCord: 32
    }
    worker F {
        int y = <- B;
        y -> function;
    }

    int y1 = <- C;
    int y2 = <- D;
    int y3 = <- E;
    int y4 = <- F;
}
