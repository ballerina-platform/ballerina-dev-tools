
@display {
    label: "Flow",
    id: "2",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "CloneNode",
        xCord: 11,
        yCord: 32
    }
    worker A {
        int x = 12;
        x -> B;
    }

    @display {
        label: "Node",
        templateId: "CodeBlockNode",
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
