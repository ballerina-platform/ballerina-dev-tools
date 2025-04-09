/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

module io.ballerina.architecturemodelgenerator {
    requires com.google.gson;
    requires io.ballerina.lang;
    requires io.ballerina.parser;
    requires io.ballerina.tools.api;
    requires io.ballerina.language.server.commons;
    requires java.desktop;
    requires io.ballerina.model.generator.commons;
    requires io.ballerina.runtime;

    exports io.ballerina.architecturemodelgenerator.core;
    exports io.ballerina.architecturemodelgenerator.core.diagnostics;
    exports io.ballerina.architecturemodelgenerator.core.model;
    exports io.ballerina.architecturemodelgenerator.core.model.common;
    exports io.ballerina.architecturemodelgenerator.core.model.entity;
    exports io.ballerina.architecturemodelgenerator.core.model.service;
    exports io.ballerina.architecturemodelgenerator.core.model.functionentrypoint;
    exports io.ballerina.architecturemodelgenerator.core.generators.entity;
    exports io.ballerina.designmodelgenerator.core;
    exports io.ballerina.designmodelgenerator.core.model;
    exports io.ballerina.artifactsgenerator;
}
