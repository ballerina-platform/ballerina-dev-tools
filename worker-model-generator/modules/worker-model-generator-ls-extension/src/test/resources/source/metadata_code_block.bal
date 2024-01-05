
@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() {
    @display {
        label: "Node",
        templateId: "CodeBlockNode",
        xCord: 11,
        yCord: 32,
        metadata: "{input: {type: json, name: in}, output: {type: string, name: out}}"
    }
    worker A {
    }
}
