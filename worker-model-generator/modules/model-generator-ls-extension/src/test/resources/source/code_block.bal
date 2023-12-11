
@display {
    label: "Flow",
    id: "2",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "block",
        xCord: 11,
        yCord: 32
    }
    worker A {
        12 -> B;
    }

    @display {
        label: "Node",
        templateId: "block",
        xCord: 32,
        yCord: 63
    }
    worker B {
        int x = <- A;
        int a = 32 + x;
        int b = a % 12;
        b -> function;
    }

    int result = <- B;
}
