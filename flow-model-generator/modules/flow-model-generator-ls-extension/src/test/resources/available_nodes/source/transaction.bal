public function testAvailableNodesInTransactionBody1() returns () {
    do {
        transaction {
            check commit;
        }
    } on fail error e {
    	
    }
}
