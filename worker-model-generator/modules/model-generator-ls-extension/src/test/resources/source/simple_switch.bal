
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
        int x = 11;
        x -> B;
    }

    @display {
        label: "Node",
        templateId: "switch",
        xCord: 12,
        yCord: 3
    }
    worker B {
        int x = <- A;

        if (x > 10) {
            x -> C;
        } else {
            x -> D;
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
        x -> function;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 12,
        yCord: 56
    }
    worker D {
        int x = <- B;
        x -> function;
    }

    int y1 = <- C;
    int y2 = <- D;
}
