public function deleteElseIf1() {
    if true {
        int i = 2;
    } else if true {
        int j = 2;
    } else {
        int k = 4;
    }
}

public function deleteElseIf2() {
    if true {
        int i = 2;
    } else if true {
        int j = 2;
        int k = 3;
    }
}

public function deleteElseIf3() {
    if true {
        int i = 2;
    } else {
        int j = 2;
        if true {
            int i = 2;
        } else if {
            int j = 2;
        } else {
            int k = 4;
        }
    }
}
