
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Worker",
        templateId: "CloneNode",
        xCord: 32,
        yCord: 64
    }
    worker A {
        int x = <- function;
        x -> C;
    }

    @display {
        label: "Worker",
        templateId: "CloneNode",
        xCord: 32,
        yCord: 30
    }
    worker B {
        int y = <- function;
        y -> C;
    }

    @display {
        label: "Worker",
        templateId: "ReplyNode",
        xCord: 100,
        yCord: 102
    }
    worker C {
        int y = <- A | B;
        y -> function;
    }

    3 -> A;
    5 -> B;
    _ = <- C;
}
