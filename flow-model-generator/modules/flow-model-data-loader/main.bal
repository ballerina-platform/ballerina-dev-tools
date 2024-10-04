import ballerina/data.jsondata;
import ballerina/file;
import ballerina/http;
import ballerina/io;
import ballerina/log;

const string PATH_SOURCE = "../source_data/";
const string PATH_INDEX_FUNCTIONS = "../index/function/";
const string PATH_INDEX_CONNECTIONS = "../index/connection/";

const string PATH_INDEX = "../flow-model-generator-ls-extension/src/main/resources/";
const string PATH_CONNECTION_JSON = "connections.json";
const string PATH_CONNECTOR_JSON = "connectors.json";
const string PATH_FUNCTION_JSON = "functions.json";
const string PATH_NODE_TEMPLATE_JSON = "node_templates.json";

final DataSet prebuiltDataSet = check jsondata:parseStream(check io:fileReadBlocksAsStream(PATH_SOURCE + "index.json"));
final http:Client gqlCL = check new ("https://api.central.ballerina.io/2.0/graphql");

public function main() returns error? {
    check fetchDataFromRemoteAPI();
}

function fetchDataFromRemoteAPI() returns error? {

    final map<ModuleConfig> modules = {}; // Unique modules.
    final map<string> orgs = {}; // Unique orgs.

    readPreBuiltDataAndBuildCache(modules, orgs);

    // Fetch the modules.
    foreach var org in orgs {
        string orgFile = getCachedDataFilePath(org);
        GQLPackagesResponse res;
        if check file:test(orgFile, file:EXISTS) {
            log:printInfo("Using cached data for org: " + org);
            res = check jsondata:parseStream(check io:fileReadBlocksAsStream(orgFile));
        } else {
            json request = {operationName: null, variables: {}, query: string `{  query: packages(orgName: "${org}", limit: 1000) { packages { organization name version icon keywords modules { name } } }}`};
            res = check gqlCL->post("", request);
            check io:fileWriteJson(orgFile, res.toJson());
        }

        foreach PackagesItem pkg in res.data.query.packages {
            foreach ModulesItem module in pkg.modules {
                final string key = getModuleQName(pkg.organization, module.name);
                if !modules.hasKey(key) {
                    continue;
                }
                ModuleConfig config = modules.get(key);
                config.icon = pkg.icon;
                string moduleFile = getCachedDataFilePath(key);
                GQLDocsResponse docRes;
                if check file:test(moduleFile, file:EXISTS) {
                    docRes = check (check io:fileReadJson(moduleFile)).cloneWithType();
                } else {
                    json docRequest = {
                        "operationName": null,
                        "variables": {},
                        "query": "{\n  query: apiDocs(inputFilter: {moduleInfo: {orgName: \"" + pkg.organization + "\", moduleName: \"" + module.name + "\", version: \"" + pkg.version + "\"}}) {\n    docsData {\n      modules {\n        clients\n        listeners\n        functions\n      }\n    }\n  }\n}\n"
                    };
                    docRes = check gqlCL->post("", docRequest);
                    // Convert the json strings to json objects.
                    foreach var [itemKey, item] in docRes.data.query.docsData.modules[0].entries() {
                        if item !is string {
                            continue;
                        }
                        json data = <json>check jsondata:parseString(item);
                        docRes.data.query.docsData.modules[0][itemKey] = data;
                    }
                    check io:fileWriteJson(PATH_SOURCE + key + ".json", docRes.toJson());
                }
                check extractIndex(FUNCTIONS, config, pkg, module, key, docRes);
                check extractIndex(CONNECTIONS, config, pkg, module, key, docRes);
            }
        }
    }

    Index index = {};
    check buildConnectorIndex(modules, index);
    check buildFunctionIndex(modules, index);

    check io:fileWriteJson(PATH_INDEX + PATH_NODE_TEMPLATE_JSON, index.nodeTemplates);
    check io:fileWriteJson(PATH_INDEX + PATH_CONNECTOR_JSON, index.clients);
    check io:fileWriteJson(PATH_INDEX + PATH_CONNECTION_JSON, index.connections);
    check io:fileWriteJson(PATH_INDEX + PATH_FUNCTION_JSON, index.functions);
}

enum DataGroupType {
    CONNECTIONS,
    FUNCTIONS
}

function extractIndex(DataGroupType dgt, ModuleConfig config, PackagesItem pkg, ModulesItem module, string key, GQLDocsResponse docRes) returns error? {
    DataGroup[] dataGroups = dgt == CONNECTIONS ? prebuiltDataSet.connections : prebuiltDataSet.functions;
    string[] symbolNames = from DataGroup dataGroup in dataGroups
        from DataItem dataItem in dataGroup.items
        where dataItem.ref[0] == pkg.organization && dataItem.ref[1] == module.name
        select dataItem.ref[2];

    final var moduleData = docRes.data.query.docsData.modules[0];
    json[] symbolDataList = from string symbolName in symbolNames
        from json data in <json[]>(dgt == CONNECTIONS ? moduleData.clients : moduleData.functions)
        where symbolName == data.name
        select data;

    final string filePath = dgt == CONNECTIONS ? PATH_INDEX_CONNECTIONS : PATH_INDEX_FUNCTIONS;
    foreach var symbolData in symbolDataList {
        json projectedData;
        removeUnwantedFields(symbolData);
        // Project Data for reducing the size of the json.
        if dgt == CONNECTIONS {
            ClientInfo clientData = check jsondata:parseAsType(symbolData);
            if !clientData.hasKey("initMethod") {
                string[] names = from FunctionInfo f in clientData.methods
                    select f.name;
                string nameList = string:'join(",", ...names);
                string msg = string `initMethod not found for client: ${key} - ${clientData.name}. Available methods: ${nameList}`;
                log:printInfo(msg);
            }
            foreach var data in clientData.remoteMethods {
                processParameters(data);
            }
            clientData.icon = pkg.icon;
            projectedData = clientData;
            config.clients.push(clientData);
        } else {
            FunctionInfo data = check jsondata:parseAsType(symbolData);
            data.icon = pkg.icon;
            projectedData = data;
            processParameters(data);
            config.functions.push(data);
        }
        check io:fileWriteJson(filePath + key + "_" + <string>check symbolData.name + ".json", projectedData);
    }
}

function processParameters(FunctionInfo data) {
    // io:println("Method/Function: " + data.name);
    foreach var param in data.parameters ?: [] {
        var typeData = parseAsType(param.'type);
        param.defaultValueCalculated = typeData.defaultValue;
        param.toString = typeData.name;
        param.importStmt = typeData.importStmt;
        // io:println("    Param: " + (param.toString ?: "") + " - " + typeData.name + " - " + typeData.defaultValue + " - " + (typeData.importStmt ?: ""));
    }
    var returnParameters = data.returnParameters;
    if returnParameters != () && returnParameters.length() > 0 {
        var typeData = parseAsType((data.returnParameters ?: [])[0].'type);
        data.returnParameters[0].defaultValueCalculated = typeData.defaultValue;
        data.returnParameters[0].toString = typeData.name;
        data.returnParameters[0].importStmt = typeData.importStmt;
        // io:println("    Return: " + typeData.name + " - " + (typeData.importStmt ?: ""));
    }
}

// function generateClinetAIDescription(ClientInfo data) returns error? {

//     foreach FunctionInfo func in data.remoteMethods {
//         // Generate the method name.
//         string methodName = func.name;
//         var retrunParam = func.returnParameters;
//         if retrunParam !is () && retrunParam.length() > 0 {
//             TypeInfo typeInfo = retrunParam[0].'type;
//             if typeInfo.isAnonymousUnionType == true
//                 && let var members = typeInfo.memberTypes in members != () {
//                 // foreach var memberType in members {

//                 // }
//             }
//         }
//         string methodExample = string `CLIENT_NAME->${methodName}()`;
//     }
//     string aiDescription = string `
//     // Avialable remote methods.
//     `;
// }

function removeUnwantedFields(json data) {
    final string[] unwantedFields = ["generateUserDefinedTypeLink"];
    if data is json[] {
        foreach json j in data {
            removeUnwantedFields(j);
        }
    } else if data is map<json> {
        foreach var [key, value] in data.entries() {
            if value is false {
                _ = data.remove(key);
            } else if value is json[] && value.length() == 0 {
                _ = data.remove(key);
            } else if unwantedFields.indexOf(key) != () {
                _ = data.remove(key);
            } else if value is 0 {
                if key == "arrayDimensions" {
                    _ = data.remove(key);
                }
            } else {
                removeUnwantedFields(value);
            }
        }
    }
}

function readPreBuiltDataAndBuildCache(map<ModuleConfig> modules, map<string> orgs) {
    // Build a list of modules to fetch, from the ref in the groups.
    foreach DataGroup[] val in prebuiltDataSet {
        var groups = <DataGroup[]>val; // JBug: Union of the same type is not iterable.
        foreach DataGroup group in groups {
            foreach DataItem data in group.items {
                if data.enabled == false {
                    continue;
                }
                final var [orgName, moduleName, _] = data.ref;
                final string moduleQName = getModuleQName(orgName, moduleName);

                ModuleConfig config;
                if modules.hasKey(moduleQName) {
                    config = modules.get(moduleQName);
                } else {
                    config = {orgName, moduleName};
                }
                modules[moduleQName] = config;
                orgs[orgName] = orgName;
            }
        }
    }
}

function getModuleQName(string org, string module) returns string {
    return org + "/" + module;
}

function getCachedDataFilePath(string cache) returns string {
    return string `${PATH_SOURCE}${cache}.json`;
}

type ModuleConfig record {|
    string orgName;
    string moduleName;
    string icon = "";
    ClientInfo[] clients = [];
    FunctionInfo[] functions = [];
|};

type ModulesItem record {
    string name;
};

type PackagesItem record {
    string organization;
    string name;
    string version;
    string icon;
    string[] keywords;
    ModulesItem[] modules;
};

type Query record {
    PackagesItem[] packages;
};

type Data record {
    Query query;
};

type GQLPackagesResponse record {
    Data data;
};

type DocsModulesItem record {
    string|json[] clients;
    string|json[] listeners;
    string|json[] functions;
};

type DocsDataItem record {
    DocsModulesItem[] modules;
};

type DoscQuery record {
    DocsDataItem docsData;
};

type DocsData record {
    DoscQuery query;
};

type GQLDocsResponse record {
    DocsData data;
};

type DataItem record {|
    string label;
    [string, string, string] ref;
    string[] popular?; // TODO: implement this.
    boolean enabled?;
|};

type DataGroup record {|
    string label;
    DataItem[] items;
|};

type DataSet record {|
    DataGroup[] connections;
    DataGroup[] functions;
|};

type ClientInfo record {|
    FunctionInfo[] remoteMethods;
    FunctionInfo initMethod?;
    FunctionInfo[] methods;
    string name;
    string description;
    boolean isDeprecated?;
    string icon?;
    string ai_description?;
|};

type FunctionInfo record {|
    boolean isRemote?;
    ParameterInfo[] parameters?;
    ParameterInfo[] returnParameters?;
    boolean isDeprecated?;
    string name;
    string description;
    string icon?;
    TypeInfo dependentlyTyped?;
|};

type ParameterInfo record {|
    string defaultValue?;
    string defaultValueCalculated?;
    TypeInfo 'type;
    string name;
    string description;
    string toString?;
    string importStmt?;
|};

type TypeInfo record {|
    string name?;
    string category?;
    boolean isInclusion?;
    boolean isAnonymousUnionType?;
    boolean isArrayType?;
    boolean isNullable?;
    boolean isTuple?;
    boolean isTypeDesc?;
    boolean isIntersectionType?;
    boolean isRestParam?;
    TypeInfo[] memberTypes?;
    int arrayDimensions?;
    TypeInfo constraint?; // For Maps
    TypeInfo elementType?; // For dependent types
    json...;
|};

// Type Info

type TypeInfoDetails record {
    string name;
    string defaultValue;
    string importStmt?;
};

function parseAsType(TypeInfo? typeInfo) returns TypeInfoDetails {
    if typeInfo == () {
        return {name: "undefined", defaultValue: ""};
    }
    string typeName = "undefined";
    string defaultValue = "";
    string? importStmt = ();
    match typeInfo {
        {category: "builtin", isNullable: true} => {
            typeName = (typeInfo.name ?: "") + "|()";
            defaultValue = "null";
            return {name: typeName, defaultValue, importStmt};
        }
        {category: "map", name: "map"} => {
            var constraint = parseAsType(typeInfo.constraint);
            importStmt = appendImport(importStmt, constraint.importStmt);
            typeName = string `map<${constraint.name}>`;
            defaultValue = "{}";
            return {name: typeName, defaultValue, importStmt};
        }
        var {isArrayType, arrayDimensions, elementType} if isArrayType == true => {
            var element = parseAsType(elementType);
            importStmt = appendImport(importStmt, element.importStmt);
            string arraySyntax = "";
            foreach var _ in 1 ... arrayDimensions {
                arraySyntax = arraySyntax + "[]";
            }
            typeName = string `${element.name}${arraySyntax}`;
            defaultValue = "[]";
            return {name: typeName, defaultValue, importStmt};
        }
        {category: "builtin", name: _} => {
            typeName = (typeInfo.name ?: "");
            if typeName == "string" {
                defaultValue = string `""`;
            } else if typeName == "int" {
                defaultValue = "0";
            } else if typeName == "float" {
                defaultValue = "0.0";
            } else if typeName == "decimal" {
                defaultValue = "0.0d";
            } else if typeName == "boolean" {
                defaultValue = "false";
            } else if typeName == "json" {
                defaultValue = "{}";
            } else if typeName == "xml" {
                defaultValue = "<data></data>";
            } else if typeName == "anydata" || typeName == "any" {
                defaultValue = "{}";
            } else if typeName == "readonly" {
                defaultValue = "()";
            }
            return {name: typeName, defaultValue, importStmt};
        }
        {category: "reference"} => {
            typeName = typeInfo.name ?: "";
            defaultValue = "";
            return {name: typeName, defaultValue, importStmt};
        }
        var {isTypeDesc, elementType} if isTypeDesc == true => {
            var element = parseAsType(elementType);
            importStmt = appendImport(importStmt, element.importStmt);
            typeName = string `typedesc<${element.name}>`;
            defaultValue = element.defaultValue;
            return {name: typeName, defaultValue, importStmt};
        }
        {category: "inline_record"} => {
            typeName = "record {}";
            defaultValue = "{}";
            return {name: typeName, defaultValue, importStmt};
        }
        var {isTuple, memberTypes} if isTuple == true => {
            string tupleTypes = "";
            foreach var item in memberTypes {
                var typeData = parseAsType(item);
                importStmt = appendImport(importStmt, typeData.importStmt);
                if tupleTypes == "" {
                    tupleTypes = typeData.name;
                } else {
                    tupleTypes = tupleTypes + "," + typeData.name;
                }
            }
            typeName = string `[${tupleTypes}]`;
            defaultValue = "[]";
            return {name: typeName, defaultValue, importStmt};
        }
        var {category, moduleName, orgName, name} if moduleName is string && orgName is string => {
            boolean isNullable = typeInfo.isNullable == true;
            if category == "errors" {
                defaultValue = isNullable ? "()" : string `error ("error message")`; // Check if this is correct.
            } else if category == "records" {
                defaultValue = "{}";
            } else if category == "libs" {
                defaultValue = isNullable ? "()" : ""; // SQL, etc.
            }
            string prefix;
            [importStmt, prefix] = getImportStmt([orgName, moduleName, typeName]);
            name = string `${prefix}:${name}` + (isNullable ? "|()" : "");
            return {name, defaultValue, importStmt};
        }
        {category: "error", isNullable: true} => {
            typeName = "error" + "|()";
            defaultValue = "null";
            return {name: typeName, defaultValue, importStmt};
        }
        {category: "error"} => {
            typeName = "error";
            defaultValue = string `error ("error message")`;
            return {name: typeName, defaultValue, importStmt};
        }
        var {isAnonymousUnionType, memberTypes} if isAnonymousUnionType == true => {

            string unionTypes = "";
            foreach var memberType in memberTypes {
                var typeData = parseAsType(memberType);
                importStmt = appendImport(importStmt, typeData.importStmt);
                if unionTypes == "" {
                    unionTypes = typeData.name;
                } else {
                    unionTypes = unionTypes + "|" + typeData.name;
                }
            }
            typeName = unionTypes;
            defaultValue = "";
            return {name: typeName, defaultValue, importStmt};
        }
        var {isIntersectionType, memberTypes} if isIntersectionType == true => {
            string intersectionTypes = "";
            foreach var memberType in memberTypes {
                var typeData = parseAsType(memberType);
                importStmt = appendImport(importStmt, typeData.importStmt);
                if intersectionTypes == "" {
                    intersectionTypes = typeData.name;
                } else {
                    intersectionTypes = intersectionTypes + "&" + typeData.name;
                }
            }
            typeName = intersectionTypes;
            defaultValue = "";
            return {name: typeName, defaultValue, importStmt};
        }
        var {category, memberTypes} if category == "stream" => {
            string streamTypes = "";
            foreach var item in memberTypes {
                var typeData = parseAsType(item);
                importStmt = appendImport(importStmt, typeData.importStmt);
                if streamTypes == "" {
                    streamTypes = typeData.name;
                } else {
                    streamTypes = streamTypes + "|" + typeData.name; // Check this. 
                }
            }
            typeName = string `stream<${streamTypes}>`;
            defaultValue = "";
            return {name: typeName, defaultValue, importStmt};
        }
    }
    if typeName == "undefined" {
        log:printError("Type not found: " + typeInfo.toString());
    }
    return {name: typeName, defaultValue, importStmt};
}

function appendImport(string? importStmt, string? newImports) returns string {
    if importStmt == () {
        return newImports ?: "";
    }
    if newImports == () {
        return importStmt;
    }
    if !newImports.includes(importStmt) {
        return importStmt + "\n" + newImports;
    }
    return importStmt;
}

function getImportStmt([string, string, string]|[string, string] ref) returns [string, string] {
    string prefix = ref[1];
    if ref[1].includes(".") {
        prefix = ref[1].substring(<int>ref[1].lastIndexOf(".") + 1);
    }
    return [string `import ${ref[0]}/${ref[1]} as ${prefix};`, prefix];
}

// Generate index for the functions

type ModuleConfigMap map<ModuleConfig>;

type Index record {|
    IndexAvilableNodes clients = {items: []};
    IndexConnectionNodes connections = {};
    IndexAvilableNodes functions = {items: []};
    IndexNodeTemplateMap nodeTemplates = {};
|};

type IndexMetadata record {|
    string label;
    string description?;
    string[] keywords?;
    string icon?;
    boolean active?;
    json...;
|};

type IndexCodedata record {|
    string node;
    string module;
    string symbol;
    string org;
    string 'object?;
    json...;
|};

type IndexNode record {|
    IndexMetadata metadata;
    IndexCodedata codedata;
    boolean enabled?;
|};

type IndexCategory record {|
    IndexMetadata metadata;
    IndexNode[]|IndexCategory[] items;
|};

type IndexAvilableNodes record {|
    IndexCategory[] items;
|};

type IndexConnectionNodes record {|
    IndexNode[]...;
|};

type IndexProperty record {|
    IndexMetadata metadata;
    string valueType;
    string value;
    boolean optional?;
    boolean editable?;
    boolean advanced?;
    json valueTypeConstraints?;
    int 'order;
    string group?;
|};

type IndexPropertyGroup record {|
    string group?;
    IndexMetadata metadata;
    map<IndexProperty|IndexPropertyGroup> properties = {};
|};

// type IndexProperties map<IndexProperty|IndexPropertyGroup>;
type IndexProperties map<IndexProperty>;

type IndexNodeTemplate record {|
    IndexMetadata metadata;
    IndexCodedata codedata;
    IndexProperties properties;
    int flags;
|};

type IndexNodeTemplateMap map<IndexNodeTemplate>;

function buildFunctionIndex(ModuleConfigMap modulesConfigMap, Index index) returns error? {

    foreach DataGroup dataGroup in prebuiltDataSet.functions {
        final IndexCategory indexCategory = {
            metadata: {label: dataGroup.label},
            items: <IndexNode[]>[]
        };
        index.functions.items.push(indexCategory);
        foreach DataItem dataItemConnection in dataGroup.items {
            check buildFunctionIndexForDataItem(modulesConfigMap, index, indexCategory, dataItemConnection);
        }
    }
}

function buildConnectorIndex(ModuleConfigMap modulesConfigMap, Index index) returns error? {

    foreach DataGroup dataGroup in prebuiltDataSet.connections {
        final IndexCategory indexCategory = {
            metadata: {label: dataGroup.label},
            items: <IndexNode[]>[]
        };
        index.clients.items.push(indexCategory);
        foreach DataItem dataItemConnection in dataGroup.items {
            check buildConnectorIndexForDataItem(modulesConfigMap, index, indexCategory, dataItemConnection);
        }
    }
}

// Build Connector Index. Can be refactor futher.

function loadCachedClient(ModuleConfig moduleConfig, string clientName) returns ClientInfo? {
    foreach var item in moduleConfig.clients {
        if item.name == clientName {
            return item;
        }
    }
    return ();
}

function loadCachedFunction(ModuleConfig moduleConfig, string functionName) returns FunctionInfo? {
    foreach var item in moduleConfig.functions {
        if item.name == functionName {
            return item;
        }
    }
    return ();
}

function buildConnectorIndexForDataItem(ModuleConfigMap modulesConfigMap, Index index, IndexCategory indexCategory, DataItem dataItemConnection) returns error? {

    final var [orgName, moduleName, clientName] = dataItemConnection.ref;

    final ModuleConfig moduleConfig = modulesConfigMap.get(getModuleQName(orgName, moduleName));
    final ClientInfo? clientItem = <ClientInfo?>loadCachedClient(moduleConfig, clientName);

    if clientItem == () {
        log:printWarn("Client data not found for module: ", mod = moduleName, cl = clientItem);
        return ();
    }

    final string connectionsDescriptions = clientItem.description;

    // handle Init method
    final IndexNodeTemplate initTemplate = handleInitMethod(dataItemConnection.ref, clientItem);
    final string connectorKey = string `NEW_CONNECTION:${orgName}:${moduleName}:${clientName}:init`;
    index.nodeTemplates[connectorKey] = initTemplate;
    initTemplate.metadata.icon = moduleConfig.icon;

    final IndexNode connectorNode = {
        metadata: {label: dataItemConnection.label, description: connectionsDescriptions, icon: moduleConfig.icon},
        codedata: initTemplate.codedata,
        enabled: true
    };
    indexCategory.items.push(connectorNode);

    // Handle Connection Actions
    final IndexNode[] actionNodes = [];
    index.connections[connectorKey] = actionNodes;

    final IndexNodeTemplate[] templates = handleRemoteMethods(dataItemConnection.ref, clientItem);
    foreach IndexNodeTemplate template in templates {
        actionNodes.push({
            metadata: template.metadata,
            codedata: template.codedata,
            enabled: true
        });
        index.nodeTemplates[string `ACTION_CALL:${orgName}:${moduleName}:${clientName}:${template.codedata.symbol}`] = template;
        template.metadata.icon = moduleConfig.icon;
    }

    // TODO sort the actions based on the popularity and name.
}

function buildFunctionIndexForDataItem(ModuleConfigMap modulesConfigMap, Index index, IndexCategory indexCategory, DataItem dataItemConnection) returns error? {

    final var [orgName, moduleName, functionName] = dataItemConnection.ref;

    final ModuleConfig moduleConfig = modulesConfigMap.get(getModuleQName(orgName, moduleName));
    final FunctionInfo? functionItem = <FunctionInfo?>loadCachedFunction(moduleConfig, functionName);

    if functionItem == () {
        log:printWarn("Function data not found for module: ", mod = moduleName, cl = functionItem);
        return ();
    }

    final string connectionsDescriptions = functionItem.description;

    IndexNodeTemplate template = handleFunction([orgName, moduleName, functionName], functionItem);
    index.nodeTemplates[string `FUNCTION_CALL:${orgName}:${moduleName}:${functionName}`] = template;
    template.metadata.icon = moduleConfig.icon;

    final IndexNode connectorNode = {
        metadata: {label: dataItemConnection.label, description: connectionsDescriptions, icon: moduleConfig.icon},
        codedata: template.codedata,
        enabled: true
    };
    indexCategory.items.push(connectorNode);
}

function handleInitMethod([string, string, string] ref, ClientInfo connection) returns IndexNodeTemplate {

    final var [org, module, 'object] = ref;
    final [string, string] [importStmt, prefix] = getImportStmt(ref);

    // Only works here, since this is an init method. 
    // Also we assume no client has defined int the current module.
    final string returnTypeName = prefix + ":" + 'object;

    final IndexNodeTemplate initTemplate = getConnectionInitTempate(org, module, 'object);
    final IndexProperties properties = initTemplate.properties;

    initTemplate.codedata["importStmt"] = importStmt;

    // Find init method.
    FunctionInfo? init = connection.initMethod;
    if init is () {
        // Check for method parameters as old module doc has this.
        log:printWarn("Old Module conente found", 'source = string `${org}/${module}:${'object}:init`);
        FunctionInfo[] methods = connection.methods;
        if methods.length() == 0 || methods.filter(m => m.name == "init").length() == 0 {
            // No explicit init method found
        } else {
            init = methods.filter(m => m.name == "init")[0];
        }
    }

    if init !is () {
        // Add method parameters as properties
        processParameters(init);
        handleFunctionParameters(init, properties, false);
        if handleInitMethodReturn(init).length() > 0 {
            // We always Check errors. So we set the flag.
            // TODO: Improve this logic.
            setCheckedFlag(initTemplate);
        }
    }

    final function () returns int getOrder = getOrderFunction();
    // Support Variable Definition & Assignment
    // IndexPropertyGroup variablePropertyGroup = getVariablePropertyGroup(returnTypeName, getOrder);
    // properties["Variable"] = variablePropertyGroup;
    // Following are temporary fix.
    IndexPropertyGroup propertyGroup = getNewVariablePropertyGroup(returnTypeName, getOrder);
    foreach var [key, value] in propertyGroup.properties.entries() {
        properties[key.toLowerAscii()] = <IndexProperty>value;
    }

    properties["scope"] = getPropertyScope('order = getOrder());

    // TODO: Check init contains errors. Use category field. Following is a temporary fix. 
    return initTemplate;
}

function handleInitMethodReturn(FunctionInfo method) returns string[] {

    // Why? 
    // Init method return type does not contain Object type in the function signature.
    // It is always subtype of `error?` including `()`.
    // Also we should remove `()` from the return type.
    // Somehow `()` is not contained in the return type in the current implementation.
    // Which makes the implementation easier for now.
    // So return type is always list of error types.

    // Assume no dependently typed functions for now.
    if (method.returnParameters ?: []).length() > 0 {
        // TODO: handle error types later.
        return ["error"];
    }
    return [];
}

function handleRemoteMethods([string, string, string] ref, ClientInfo connection) returns IndexNodeTemplate[] {
    IndexNodeTemplate[] templates = [];
    final var [org, module, 'object] = ref;
    final [string, string] [importStmt, prefix] = getImportStmt(ref);

    FunctionInfo[] methods = connection.remoteMethods;
    foreach FunctionInfo method in methods {
        IndexNodeTemplate template = getRemoteActionTempate(org, module, 'object, <string>method.name, method.description);
        final IndexProperties properties = template.properties;
        templates.push(template);

        // TODO: Check function contains errors. 
        setCheckedFlag(template);

        handleFunctionParameters(method, template.properties);

        string returnTypeName = handleDependentType(method);

        final function () returns int getOrder = getOrderFunction();
        // Support Variable Definition & Assignment
        // IndexPropertyGroup variablePropertyGroup = getVariablePropertyGroup(returnTypeName, getOrder);
        // properties["Variable"] = variablePropertyGroup;
        // Following are temporary fix.
        IndexPropertyGroup propertyGroup = getNewVariablePropertyGroup(returnTypeName, getOrder);
        foreach var [key, value] in propertyGroup.properties.entries() {
            properties[key.toLowerAscii()] = <IndexProperty>value;
        }

        // Handle Connection Property
        properties["connection"] = getConnectionProperty("connection", prefix + ":" + 'object, 'order = getOrder());
    }

    // TODO: Sort based on popularity and name.
    return templates;
}

function handleFunction([string, string, string] ref, FunctionInfo func) returns IndexNodeTemplate {
    IndexNodeTemplate template = {
        metadata: {label: <string>func.name, description: func.description},
        codedata: {node: "FUNCTION_CALL", module: ref[1], symbol: <string>func.name, org: ref[0]},
        properties: {},
        flags: 0
    };
    string prefix = ref[1];
    if ref[1].includes(".") {
        prefix = ref[1].substring(<int>ref[1].lastIndexOf(".") + 1);
    }
    template.codedata["importStmt"] = "import " + ref[0] + "/" + ref[1] + " as " + prefix;
    handleFunctionParameters(func, template.properties);

    string returnTypeName = handleDependentType(func);

    final function () returns int getOrder = getOrderFunction();
    // Support Variable Definition & Assignment
    // IndexPropertyGroup variablePropertyGroup = getVariablePropertyGroup(returnTypeName, getOrder);
    // properties["variable"] = variablePropertyGroup;
    // Following are temporary fix.
    IndexPropertyGroup propertyGroup = getNewVariablePropertyGroup(returnTypeName, getOrder);
    foreach var [key, value] in propertyGroup.properties.entries() {
        template.properties[key] = <IndexProperty>value;
    }

    // TODO: Check init contains errors. Use category field. Following is a temporary fix. 
    setCheckedFlag(template);
    return template;
}

function handleDependentType(FunctionInfo func) returns string {
    string returnTypeName = "string";
    if func.dependentlyTyped !is () {
        TypeInfo returnType = <TypeInfo>func.dependentlyTyped;
        returnTypeName = getDefaultDependetType(returnType.elementType);
    }
    return returnTypeName;
}

function getDefaultDependetType(TypeInfo? typeInfo) returns string {
    match typeInfo {
        {name: "anydata", category: "builtin"} => {
            return "json";
        }
        {category: "inline_record"} => {
            return "map<json>";
        }
    }
    return "json";
}

function handleFunctionParameters(FunctionInfo method, IndexProperties properties, boolean handleReturn = true) {
    foreach ParameterInfo item in (method.parameters ?: []) {
        if handleReturn && item.defaultValue == "<>" {
            // This is dependently Typed function
            method.dependentlyTyped = item.'type;
            continue;
        }
        boolean optional = item.defaultValue != () && item.defaultValue != "";
        string defaultValue = item.defaultValue ?: "";
        if !optional {
            defaultValue = item.defaultValueCalculated ?: "";
        }
        properties[item.name] = {
            metadata: {label: item.name, description: item.description},
            valueType: "Expression",
            value: defaultValue,
            optional,
            editable: true,
            valueTypeConstraints: {'type: item.'type.toJson()},
            'order: properties.length()
        };
    }
}

// Helper functions

function setCheckedFlag(IndexNodeTemplate template) {
    template.flags = template.flags | 1;
    if template.codedata.hasKey("flags") {
        template.codedata["flags"] = <int>template.codedata["flags"] | 1;
    } else {
        template.codedata["flags"] = 1;
    }
}

function getOrderFunction() returns function () returns int {
    int 'order = 0;
    return function() returns int {
        'order = 'order + 1;
        return 'order;
    };
}

// Templates Helper Function

type TemplateParams record {|
    string node;
    string org;
    string module;
    string 'object?;
    string symbol;
    string? label = ();
    string? description = ();
|};

function getConnectionInitTempate(string org, string module, string 'object) returns IndexNodeTemplate {
    return createTemplate(node = "NEW_CONNECTION", label = "New Connection", description = "Create a new connection", org = org, module = module, 'object = 'object, symbol = "init");
}

function getRemoteActionTempate(string org, string module, string 'object, string symbol, string description = "Call remote action") returns IndexNodeTemplate {
    return createTemplate(node = "ACTION_CALL", label = symbol, description = description, org = org, module = module, 'object = 'object, symbol = symbol);
}

function createTemplate(*TemplateParams params) returns IndexNodeTemplate {
    final string label = params.label ?: "";
    return {
        metadata: {label, description: params.description},
        codedata: {node: params.node, org: params.org, module: params.module, 'object: params.'object, symbol: params.symbol},
        properties: {},
        flags: 0
    };
}

// Properties Helper Functions

enum ValueType {
    ENUM = "Enum",
    TYPE = "Type",
    IDENTIFIER = "Identifier"
}

enum ConnectionScope {
    GLOBAL = "Global",
    SERVICE = "Service",
    LOCAL = "Local"
}

type PropertyParams record {|
    string? label = ();
    string? description = ();
    int 'order = -1;
    string? group = ();
    boolean? advanced = ();
    boolean? editable = ();
    boolean? optional = ();
|};

function getConnectionProperty(string value, string symbolType, *PropertyParams params) returns IndexProperty {
    params.label = params.label ?: "Connection";
    params.description = params.description ?: "Connection to use";
    params.group = params.group ?: "Core";
    params.advanced = true;
    return createSymbolProperty(value, symbolType, false, params);
}

function getVariablePropertyGroup(string returnTypeName, function () returns int getOrder) returns IndexPropertyGroup {
    IndexPropertyGroup variable = {group: "Core", metadata: {label: "Variable", description: "Variable to store result"}};
    variable.properties["new_variable"] = getNewVariablePropertyGroup(returnTypeName, getOrder);
    variable.properties["existing_variable"] = getExistingVariablePropertyGroup(returnTypeName, getOrder);
    return variable;
}

function getNewVariablePropertyGroup(string returnTypeName, function () returns int getOrder) returns IndexPropertyGroup {
    IndexPropertyGroup varDefinitionGroup = {metadata: {label: "New Variable", description: "Create a new variable"}};
    varDefinitionGroup.properties["variable"] = getPropertyVariable("value", returnTypeName, 'order = getOrder());
    varDefinitionGroup.properties["type"] = getPropertyType(returnTypeName, 'order = getOrder());
    return varDefinitionGroup;
}

function getExistingVariablePropertyGroup(string returnTypeName, function () returns int getOrder) returns IndexPropertyGroup {
    IndexPropertyGroup assignmentGroup = {metadata: {label: "Existing Variable", description: "Assign to an existing variable"}};
    assignmentGroup.properties["variable"] = getPropertyVariable("value", returnTypeName, 'order = getOrder(), assignment = true);
    return assignmentGroup;
}

function getPropertyScope(ConnectionScope value = GLOBAL, *PropertyParams params) returns IndexProperty {

    params.label = params.label ?: "Scope";
    params.description = params.description ?: "Scope of the connection";
    params.group = params.group ?: "Core";
    params.advanced = true;
    return createEnumProperty(GLOBAL, [GLOBAL, SERVICE, LOCAL], params);
}

function getPropertyVariable(string value, string symbolType, boolean assignment = false, *PropertyParams params) returns IndexProperty {

    params.label = params.label ?: "Variable";
    params.description = params.description ?: "Variable to store value";
    params.group = params.group ?: "Basic";
    return createSymbolProperty(value, symbolType, assignment, params);
}

function getPropertyType(string value, string[] typeOf = [value], *PropertyParams params) returns IndexProperty {

    params.label = params.label ?: "Type";
    params.description = params.description ?: "Type of the variable";
    params.group = params.group ?: "Basic";
    return createTypeProperty(value, typeOf, params);
}

function createEnumProperty(string value, string[] enumValues, *PropertyParams params) returns IndexProperty {

    return createProperty(value, ENUM, {'enum: enumValues}, params);
}

function createSymbolProperty(string value, string symbolType, boolean isNewVariable, *PropertyParams params) returns IndexProperty {

    json symbolContraint = {identifier: {isNewVariable, symbolType}};
    return createProperty(value, IDENTIFIER, symbolContraint, params);
}

function createTypeProperty(string value, string[] typeOf, *PropertyParams params) returns IndexProperty {

    return createProperty(value, TYPE, {typeOf}, params);
}

function createProperty(string value, ValueType valueType, json valueTypeConstraints, *PropertyParams params) returns IndexProperty {

    final string label = params.label ?: "";
    return {
        metadata: {label, description: params.description},
        value,
        optional: params.optional,
        editable: params.editable,
        advanced: params.advanced,
        'order: params.'order,
        group: params.group,
        valueType,
        valueTypeConstraints
    };
}
