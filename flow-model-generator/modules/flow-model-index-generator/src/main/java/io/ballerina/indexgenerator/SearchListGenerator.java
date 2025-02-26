
/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.indexgenerator;

import com.google.gson.Gson;
import io.ballerina.centralconnector.CentralAPI;
import io.ballerina.centralconnector.RemoteCentral;
import io.ballerina.centralconnector.response.PackageResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility class that generates a JSON file containing package metadata information from Ballerina Central. The
 * generated JSON file includes package information from both 'ballerina' and 'ballerinax' organizations.
 *
 * @since 2.0.0
 */
class SearchListGenerator {

    private static final int LIMIT = 50;
    public static final String PACKAGE_JSON_FILE = "search_list.json";
    private static final Logger LOGGER = Logger.getLogger(SearchListGenerator.class.getName());
    private static final Map<String, String> SKIPPED_PACKAGE_LIST = Map.of("ballerina", "xmldata");

    public static void main(String[] args) {
        List<PackageMetadataInfo> ballerinaPackages = getPackageList("ballerina");
        List<PackageMetadataInfo> ballerinaxPackages = getPackageList("ballerinax");

        Map<String, List<PackageMetadataInfo>> packagesMap =
                Map.of("ballerina", ballerinaPackages, "ballerinax", ballerinaxPackages);

        // Remove the skipped package list
        SKIPPED_PACKAGE_LIST.forEach((org, pkg) -> {
            packagesMap.get(org).removeIf(packageMetadataInfo -> packageMetadataInfo.name().equals(pkg));
        });

        Gson gson = new Gson();

        String destinationPath = Path.of("flow-model-generator/modules/flow-model-index-generator/src/main/resources")
                .resolve(PACKAGE_JSON_FILE)
                .toString();
        try (FileWriter writer = new FileWriter(destinationPath, StandardCharsets.UTF_8)) {
            gson.toJson(packagesMap, writer);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to write packages to JSON file", e);
        }
    }

    private static List<PackageMetadataInfo> getPackageList(String org) {
        CentralAPI centralApi = RemoteCentral.getInstance();
        PackageResponse packages = centralApi.searchPackages(Map.of(
                "org", org,
                "limit", String.valueOf(LIMIT)
        ));
        List<PackageMetadataInfo> packagesList = packages.packages().stream()
                .map(packageData -> new PackageMetadataInfo(packageData.name(), packageData.version(),
                        packageData.keywords(), packageData.pullCount()))
                .collect(Collectors.toList());
        int totalCount = packages.count();
        int totalCalls = (int) Math.ceil((double) totalCount / LIMIT);

        for (int i = 1; i < totalCalls; i++) {
            LOGGER.log(Level.INFO, "Fetching packages for {0}, offset: {1}", new Object[]{org, i * LIMIT});
            packages = centralApi.searchPackages(Map.of(
                    "org", org,
                    "limit", String.valueOf(LIMIT),
                    "offset", String.valueOf(i * LIMIT)
            ));
            packagesList.addAll(packages.packages().stream()
                    .map(packageData -> new PackageMetadataInfo(packageData.name(), packageData.version(),
                            packageData.keywords(), packageData.pullCount())).toList());
        }
        return packagesList;
    }

    record PackageMetadataInfo(String name, String version, List<String> keywords, int pullCount) { }
}
