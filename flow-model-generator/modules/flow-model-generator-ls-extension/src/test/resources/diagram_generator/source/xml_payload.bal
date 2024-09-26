function testXMLPayload() {
    xml x1 = ((xml `<p>value</p>`));
    xml:Text x2 = xml `Text`;
    xml|int x3 = xml `<p>value</p>`;
    XMLType x4 = xml `<p>value</p>`;
    var x5 = xml `<p>value</p>`;
}

type XMLType xml;
