
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
        11 -> B;
    }

    @display {
        label: "Node",
        templateId: "switch",
        xCord: 12,
        yCord: 3
    }
    worker B {
        int x = <- A;

        int y = x % 3;
        if (x < 10) {
            y -> C;
        } else if (x > 10 && x < 20) {
            y -> D;
        } else if (x > 20 && x < 40) {
            y -> E;
        } else {
            y -> F;
        }
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 10,
        yCord: 50
    }
    worker C {
        int x = <- B;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 12,
        yCord: 56
    }
    worker D {
        int x = <- B;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 13,
        yCord: 52
    }
    worker E {
        int y = <- B;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 18,
        yCord: 32
    }
    worker F {
        int y = <- B;
    }
}
