public function deleteElse1() {
    if true {
        int i = 2;
    } else {
        int j = 2;
    }
}

public function deleteElse2() {
    if true {
        int i = 2;
    } else {
        int j = 2;
        int k = 3;
    }
}

public function deleteElse3() {
    if true {
        int i = 2;
    } else {
        int j = 2;
        if true {
            int i = 2;
        } else {
            int j = 2;
        }
    }
}
