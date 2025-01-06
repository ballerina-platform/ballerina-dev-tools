public client class Client {
    
    resource function get path(boolean flag) returns string {
        do {
            if flag {
                return "path1";
            } else {
                return "path2";
            }
        }
    }

    remote function name(boolean flag) returns string {
        do {
            if flag {
                return "name1";
            } else {
                return "name2";
            }
        }
    }
    
}
