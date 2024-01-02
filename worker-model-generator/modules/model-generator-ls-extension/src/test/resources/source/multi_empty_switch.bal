
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
        } else if (x > 10 && x < 20) {

        } else if (x > 20 && x < 40) {

        } else {

        }
    }
}
