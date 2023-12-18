import ballerina/toml;

type WriteConfig record {|
    string name;
|};

@display {
    label: "Flow",
    id: "5",
    name: "main/function"
}
public function main() {

    @display {
        label: "Node",
        templateId: "clone",
        xCord: 0,
        yCord: 0
    }
    worker A {
        toml:WriteConfig writeConfig = {
            allowDottedKeys: false,
            indentationPolicy: 0
        };

        writeConfig -> C;
    }

    @display {
        label: "Node",
        templateId: "clone",
        xCord: 0,
        yCord: 10
    }
    worker B {
        WriteConfig writeConfig = {
            name: "B"
        };
        writeConfig -> C;
    }

    @display {
        label: "Node",
        templateId: "clone",
        xCord: 0,
        yCord: 20
    }
    worker C {
        toml:WriteConfig writeConfig = <- A;
        WriteConfig writeConfig2 = <- B;
    }
}
