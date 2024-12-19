/*
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

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.bala.BalaProject;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.repos.TempDirCompilationCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class that contains methods to perform package-related operations.
 *
 * @since 2.0.0
 */
public class PackageUtil {

    private static final String BALLERINA_HOME_PROPERTY = "ballerina.home";

    public static BuildProject getSampleProject() {
        // Obtain the Ballerina distribution path
        System.setProperty(BALLERINA_HOME_PROPERTY, "/Library/Ballerina/distributions/ballerina-2201.11.0");
        String ballerinaHome = System.getProperty(BALLERINA_HOME_PROPERTY);
        if (ballerinaHome == null || ballerinaHome.isEmpty()) {
            Path currentPath = getPath(Paths.get(
                    PackageUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
            Path distributionPath = getParentPath(getParentPath(getParentPath(currentPath)));
            System.setProperty(BALLERINA_HOME_PROPERTY, distributionPath.toString());
        }

        try {
            // Create a temporary directory
            Path tempDir = Files.createTempDirectory("ballerina-sample");

            // Create an empty main.bal file
            Path mainBalFile = tempDir.resolve("main.bal");
            Files.createFile(mainBalFile);

            // Create Ballerina.toml file with the specified content
            Path ballerinaTomlFile = tempDir.resolve("Ballerina.toml");
            String tomlContent = "[package]\n" +
                    "org = \"wso2\"\n" +
                    "name = \"sample\"\n" +
                    "version = \"0.1.0\"\n" +
                    "distribution = \"2201.11.0\"";
            Files.writeString(ballerinaTomlFile, tomlContent, StandardOpenOption.CREATE);
            return BuildProject.load(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while creating the sample project", e);
        }
    }

    public static Package getModulePackage(String org, String name, String version) {
        return getModulePackage(getSampleProject(), org, name, version);
    }

    public static Package getModulePackage(BuildProject buildProject, String org, String name, String version) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)));

        Collection<ResolutionResponse> resolutionResponses =
                buildProject.projectEnvironmentContext().getService(PackageResolver.class)
                        .resolvePackages(Collections.singletonList(resolutionRequest),
                                ResolutionOptions.builder().setOffline(false).build());
        Optional<ResolutionResponse> resolutionResponse = resolutionResponses.stream().findFirst();
        if (resolutionResponse.isEmpty()) {
            return null;
        }

        Path balaPath = resolutionResponse.get().resolvedPackage().project().sourceRoot();
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        BalaProject balaProject = BalaProject.loadProject(defaultBuilder, balaPath);
        return balaProject.currentPackage();
    }

    private static Path getPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null");
    }

    private static Path getParentPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null").getParent();
    }
}
