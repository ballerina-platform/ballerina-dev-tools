public function testCommit() returns error? {
    transaction {
        check commit;
    }
}