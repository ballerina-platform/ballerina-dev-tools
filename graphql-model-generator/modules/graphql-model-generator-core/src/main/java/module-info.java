module io.ballerina.graphql.model.generator {

    requires io.ballerina.lang;
    requires io.ballerina.tools.api;
    requires io.ballerina.stdlib.graphql.commons;
    requires io.ballerina.parser;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.eclipse.lsp4j;

    exports io.ballerina.graphqlmodelgenerator.core;
    exports io.ballerina.graphqlmodelgenerator.core.exception;
    exports io.ballerina.graphqlmodelgenerator.core.diagnostic;
    exports io.ballerina.graphqlmodelgenerator.core.model;

}
