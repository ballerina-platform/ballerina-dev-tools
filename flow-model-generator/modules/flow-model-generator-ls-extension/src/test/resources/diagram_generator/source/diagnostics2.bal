isolated map<boolean>[] f = [];

function fn() {
    map<boolean> bm1 = {};
    lock {
	    f = [bm1];
    }
}
