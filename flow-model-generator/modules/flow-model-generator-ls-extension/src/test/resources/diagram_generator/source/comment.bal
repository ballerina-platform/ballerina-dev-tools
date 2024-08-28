public function commentOnStatement() returns int {
    // Create variable name i
    // This is an additional comment
    int i = 0;
    return 0;
}

public function commentOnStatementBody() {
    if true {
        // This is a comment for a statement
        int i = 0;
        int x = 68;
        // This is a comment to generate flow
    }
}

public function commentInFunctionBody1() {
    int i = 0;
    // This is a function body comment
    // to generate AI flow
}

public function commentInFunctionBody2() {

    // This is a function body comment
    // to generate AI flow for function
    // `commentInFunctionBody2`
}

public function commentInElseBody1(int i) {
    if i == 3 {

    } else if i == 5 {

        // This is a comment for i = 0
        int x = 0;

        // This is a comment for if-else body
    } else {
        // This is a comment for x = 0
        int x = 0;

        // This is a comment for else body
    }
}

public function commentInWhileBody(int i) {
    int x = 0;

    // This is a while block
    while i > 3 {
        // update `x`
        x = x + 1;

        // Generate new node
    }
}

public function commentOnFailBody1(int i) {
    while i < 4 {
        _ = i + 1;
    } on fail var e {
        // This is a comment in on fail
        error err = e;

        // Panic with error `err`
    }
}

public function commentInLockBody1() {
    lock {
        // Initialize `i`
        int i = 0;

        // Lock the variable `i`
    }
}

public function commentInTransactionBody1() returns error? {
    transaction {
        // Commit the trasaction
        var res = check commit;

        // Generate new flow nodes
    }
}

public function commentInForeachBody(int i) {
    int x = 0;

    foreach var item in 1..<3 {
        // Increment `x`
        x = x + 1;

        // Precess `x`
    }
}

public function commentInDoBody(int i) {
    int x = 0;

    do {
        // Increment `x`
        x = x + 1;

        // Precess `x`
    }
}

public function emptyComment() {
    //
    int i = 0;
}

public function commentWithNewLines(int i) {
    // Initialize `x`
    // with new lines





    int x = 0;

    do {
        // Increment `x`



        x = x + 1;

        // Precess `x`



    }
}
