
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "transformer",
        xCord: 32,
        yCord: 54
    }
    worker A {
        // Use `-> W` to send a message to worker `W`.
        1 -> B;
        2 -> C;
    }

    @display {
        label: "Node",
        templateId: "transformer",
        xCord: 101,
        yCord: 12
    }
    worker B {
        // Use `<- W` to receive a message from worker `W`.
        int x1 = <- A;

        // Use `function` to refer to the function's default worker.
        x1 -> function;
    }

    @display {
        label: "Node",
        templateId: "simple",
        xCord: 12,
        yCord: 1
    }
    worker C {
        int x2 = <- A;
        x2 -> function;
    }

    int y1 = <- B;
    int y2 = <- C;

}
