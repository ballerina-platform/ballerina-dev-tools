type SequenceDiagram record {|
    # All the participants of the diagram
    Participant[] participants;
    # Source code representing the diagram
    LineRange location;
|};

type Participant record {|
    # Unique identifier of the participant
    string id;
    # Name of the participant
    string name;
    # Kind of the participant
    ParticipantKind kind;
    # Module in which the participant is defined
    string moduleName;
    # The nodes within the participant in the correct order
    Node[] nodes;
    # Source code representing the participant
    LineRange location;
|};

enum ParticipantKind {
    FUNCTION,
    WORKER,
    ENDPOINT
};

// Node
type Node record {|
    # The kind of the node
    NodeKind kind;
    # The branches of the selected node
    Branch[] branches?;
    # Properties of the node. These are specific to the given node type.
    map<json> properties?;
    # Source code representing the node
    LineRange location;
|};

type Branch record {|
    # Label of the branch
    string label;
    # Nodes within the branch
    Node[] children;
|};

enum NodeKind {
    IF,
    WHILE,
    FOREACH,
    MATCH,
    INTERACTION,
    RETURN
}

// Concrete nodes
type IfNode record {|
    *Node;
    IF kind = IF;
    record {|
        # Condition of the if node
        Expression condition;
    |} properties;
    Branch[] branches = [
        {label: "Then", children: []},
        {label: "Else", children: []}
    ];
|};

type WhileNode record {|
    *Node;
    WHILE kind = WHILE;
    record {|
        # Condition of the while node
        Expression condition;
    |} properties;
    Branch[] branches = [
        {label: "Body", children: []}
    ];
|};

type ReturnNode record {|
    *Interaction;
    RETURN kind = RETURN;
    record {|
        # Return value of the interaction
        Expression value;
    |} properties;
|};

// Interaction
type Interaction record {|
    *Node;
    # Type of the interaction
    InteractionType interactionType;
    # Participant that receives the interaction
    string targetId;
|};

enum InteractionType {
    ENDPOINT_CALL,
    FUNCTION_CALL,
    METHOD_CALL,
    WORKER_CALL
}

// Concrete interaction types
type FunctionInteraction record {|
    *Interaction;
    FUNCTION_CALL interactionType = FUNCTION_CALL;
    record {|
        # Parameters of the function
        Expression[] params;
        # Function name
        Expression name;
        # Return value of the function
        Expression value?;
    |} properties;
|};

type MethodInteraction record {|
    *Interaction;
    METHOD_CALL interactionType = METHOD_CALL;
    record {|
        # Parameters of the method
        Expression[] params;
        # Expression of the method
        Expression expr;
        # Method name
        Expression method;
        # Return value of the method
        Expression value?;
    |} properties;
|};

// Common types
type Expression record {|
    string 'type;
    string value?;
|};

type LineRange record {|
    string fileName;
    LinePosition startLine;
    LinePosition endLine;
|};

type LinePosition record {|
    int line;
    int offset;
|};
