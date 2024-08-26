import ballerina/data.jsondata;
import ballerina/file;
import ballerina/http;
import ballerina/io;
import ballerina/log;

const string PATH_SOURCE = "../source_data/";
const string PATH_INDEX_FUNCTIONS = "../index/function/";
const string PATH_INDEX_CONNECTIONS = "../index/connection/";

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
                check extractIndex(FUNCTIONS, pkg, module, key, docRes);
                check extractIndex(CONNECTIONS, pkg, module, key, docRes);
            }
        }
    }
}

enum DataGroupType {
    CONNECTIONS,
    FUNCTIONS
}

function extractIndex(DataGroupType dgt, PackagesItem pkg, ModulesItem module, string key, GQLDocsResponse docRes) returns error? {
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
        } else {
            FunctionInfo data = check jsondata:parseAsType(symbolData);
            data.icon = pkg.icon;
            projectedData = data;
            processParameters(data);
        }
        check io:fileWriteJson(filePath + key + "_" + <string>check symbolData.name + ".json", projectedData);
    }
}

function processParameters(FunctionInfo data) {
    io:println("Method/Function: " + data.name);
    foreach var param in data.parameters ?: [] {
        var typeData = parseAsType(param.'type);
        if param.defaultValue != () && param.defaultValue != "" {
            typeData.defaultValue = param.defaultValue ?: "";
            // Check <> usage.
            param.toString = typeData.name;
            param.importStmt = typeData.importStmt;
        }
        io:println("    Param: " + (param.toString ?: "") + " - " + typeData.name + " - " + typeData.defaultValue + " - " + (typeData.importStmt ?: ""));
    }
    var returnParameters = data.returnParameters;
    if returnParameters != () && returnParameters.length() > 0 {
        var typeData = parseAsType((data.returnParameters ?: [])[0].'type);
        data.returnParameters[0].toString = typeData.name;
        data.returnParameters[0].importStmt = typeData.importStmt;
        io:println("    Return: " + typeData.name + " - " + (typeData.importStmt ?: ""));
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
|};

type ParameterInfo record {|
    string defaultValue?;
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
        return {name: "undefined", defaultValue: "undefined"};
    }
    string typeName = "undefined";
    string defaultValue = "undefined";
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
                defaultValue = "null";
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
        var {category, moduleName, orgName, name} if moduleName is string && orgName is string && typeName is string => {
            boolean isNullable = typeInfo.isNullable == true;
            if category == "errors" {
                defaultValue = isNullable ? "()" : string `error ("error message")`; // Check if this is correct.
            } else if category == "records" {
                defaultValue = isNullable ? "()" : "{}";
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
            defaultValue = "undefined";
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
            defaultValue = "undefined";
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
            defaultValue = "undefined";
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
