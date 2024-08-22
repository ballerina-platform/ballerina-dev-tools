import ballerina/data.jsondata;
import ballerina/file;
import ballerina/http;
import ballerina/io;
import ballerina/log;

const string PATH_SOURCE = "../source_data/";

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
            log:printWarn("Using cached data for org: " + org);
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

                string moduleFile = getCachedDataFilePath(key);
                GQLDocsResponse docRes;
                if check file:test(moduleFile, file:EXISTS) {
                    log:printWarn("Skipping fetching docs for module: " + key);
                    continue;
                } else {
                    json docRequest = {
                        "operationName": null,
                        "variables": {},
                        "query": "{\n  query: apiDocs(inputFilter: {moduleInfo: {orgName: \"" + pkg.organization + "\", moduleName: \"" + module.name + "\", version: \"" + pkg.version + "\"}}) {\n    docsData {\n      modules {\n        clients\n        listeners\n        functions\n      }\n    }\n  }\n}\n"
                    };
                    docRes = check gqlCL->post("", docRequest);
                }
                config.icon = pkg.icon;
                foreach var [itemKey, item] in docRes.data.query.docsData.modules[0].entries() {
                    if item !is string {
                        continue;
                    }
                    json data = <json>check jsondata:parseString(item);
                    docRes.data.query.docsData.modules[0][itemKey] = data;
                }
                check io:fileWriteJson(PATH_SOURCE + key + ".json", docRes.toJson());
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
    json clients;
    json listeners;
    json functions;
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
