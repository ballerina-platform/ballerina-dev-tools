public function testAvailableNodesInFunctionBody1() returns () {
    do {
        
    }
}

public function testAvailableNodesInIfBody1() {
    do {
        int i = 0;
        if (i > 5) {

        }
    }
}

public function testAvailableNodesInFunctionBody2() returns int {
    do {

    }
    return 0;
}

class Class {
    function testAvailableNodesInMethodBody1() returns int {
        do {

        }
        return 0;
    }
}
