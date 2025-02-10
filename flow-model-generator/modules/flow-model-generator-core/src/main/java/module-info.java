/*F
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

module io.ballerina.flow.model.generator {
    requires io.ballerina.lang;
    requires io.ballerina.tools.api;
    requires io.ballerina.parser;
    requires io.ballerina.formatter.core;
    requires io.ballerina.language.server.commons;
    requires io.ballerina.language.server.core;
    requires org.eclipse.lsp4j;
    requires io.ballerina.diagram.util;
    requires io.ballerina.central.client;
    requires com.google.gson;
    requires com.graphqljava;
    requires io.ballerina.openapi.core;
    requires io.swagger.v3.oas.models;
    requires jakarta.persistence;
    requires org.xerial.sqlitejdbc;
    requires io.ballerina.toml;
    requires org.slf4j; // TODO: Remove this once the windows build issue is fixed
    requires com.fasterxml.jackson.databind;
    requires swagger.parser.v3;
    requires swagger.parser.core;
    requires org.apache.commons.lang3;
    requires java.xml;
    requires org.eclipse.lsp4j.jsonrpc;

    exports io.ballerina.flowmodelgenerator.core;
    exports io.ballerina.flowmodelgenerator.core.utils;
    exports io.ballerina.flowmodelgenerator.core.model;
    exports io.ballerina.flowmodelgenerator.core.db.model;
    exports io.ballerina.flowmodelgenerator.core.converters;
    exports io.ballerina.flowmodelgenerator.core.expressioneditor;
    exports io.ballerina.flowmodelgenerator.core.expressioneditor.services;
}
