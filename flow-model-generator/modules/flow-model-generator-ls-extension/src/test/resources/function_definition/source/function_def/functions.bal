function foo(string foo, int... bar) returns string {
    return string `Required: ${foo}, Rest: `;
}

# Divide a/b
#
# + a - Paramter a
# + b - Parameter b
function divide( int a, int b) returns int|error{
    return 2;
}

# Divide a/b
#
# + a - Paramter a
# + b - Parameter b
#
# + return - The result of the division
function divide2( int a, int b) returns int|error{
    return 2;
}
