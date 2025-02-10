import ballerina/io;
import ballerina/test;

@test:Config {groups: ["g1"]}
function testFunction1() {
    test:assertTrue(true, msg = "Failed!");
}

@test:Config {groups: ["g1", "g2"]}
function testFunction2() {
    test:assertTrue(true, msg = "Failed!");
}

@test:Config {groups: ["g2"]}
function testFunction3() {
    test:assertTrue(true, msg = "Failed!");
}
