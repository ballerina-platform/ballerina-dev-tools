import ballerina/io;
import ballerina/test;

@test:Config {}
function testFunction4() {
    test:assertTrue(true, msg = "Failed!");
}

@test:Config {groups: ["g1", "g2"]}
function testFunction5() {
    test:assertTrue(true, msg = "Failed!");
}

@test:Config {groups: []}
function testFunction6() {
    test:assertTrue(true, msg = "Failed!");
}
