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

class PackageListGenerator {

    private static final int LIMIT = 50;
    public static final String PACKAGE_JSON_FILE = "packages.json";

    public static void main(String[] args) {
        List<PackageMetadataInfo> ballerinaPackages = getPackageList("ballerina");
        List<PackageMetadataInfo> ballerinaxPackages = getPackageList("ballerinax");

        Map<String, List<PackageMetadataInfo>> packagesMap =
                Map.of("ballerina", ballerinaPackages, "ballerinax", ballerinaxPackages);
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
                .map(packageData -> new PackageMetadataInfo(packageData.name(), packageData.version()))
                .collect(Collectors.toList());
        int totalCount = packages.count();
        int totalCalls = (int) Math.ceil((double) totalCount / LIMIT);

        for (int i = 1; i < totalCalls; i++) {
            packages = centralApi.searchPackages(Map.of(
                    "org", org,
                    "limit", String.valueOf(LIMIT),
                    "offset", String.valueOf(i * LIMIT)
            ));
            packagesList.addAll(packages.packages().stream()
                    .map(packageData -> new PackageMetadataInfo(packageData.name(), packageData.version())).toList());
        }
        return packagesList;
    }

    record PackageMetadataInfo(String name, String version) { }
}
