function testBinaryData() {
    byte[] b1 = ((base64 `abcd`));
    byte[] b2 = base64 `abcd`;
    byte[]|string b3 = base64 `abcd`;
    ByteRef b4 = base64 `abcd`;
    var b5 = base64 `abcd`;
}

type ByteRef byte[];
