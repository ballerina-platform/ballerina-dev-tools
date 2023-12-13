
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "clone",
        xCord: 32,
        yCord: 54
    }
    worker A {
        int x = 2;

        x -> B;
        x -> C;
    }

    @display {
        label: "Node",
        templateId: "clone",
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
        templateId: "clone",
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
