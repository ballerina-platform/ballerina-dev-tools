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

package io.ballerina.modelgenerator.commons;

import io.ballerina.centralconnector.CentralAPI;
import io.ballerina.centralconnector.RemoteCentral;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.bala.BalaProject;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.PackageMetadataResponse;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.repos.TempDirCompilationCache;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.MessageType;

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
    private static final BuildProject SAMPLE_PROJECT = getSampleProject();

    private static final String PULLING_THE_MODULE_MESSAGE = "Pulling the module '%s' from the central";
    private static final String MODULE_PULLING_FAILED_MESSAGE = "Failed to pull the module: %s";
    private static final String MODULE_PULLING_SUCCESS_MESSAGE = "Successfully pulled the module: %s";

    public static BuildProject getSampleProject() {
        // Obtain the Ballerina distribution path
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
                    "distribution = \"2201.12.0\"";
            Files.writeString(ballerinaTomlFile, tomlContent, StandardOpenOption.CREATE);
            return BuildProject.load(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while creating the sample project", e);
        }
    }

    /**
     * Retrieves the semantic model for a given package identified by organization, name, and version.
     *
     * @param org     The organization name of the package
     * @param name    The name of the package
     * @param version The version of the package
     * @return An Optional containing the semantic model.
     */
    public static Optional<SemanticModel> getSemanticModel(String org, String name, String version) {
        return getModulePackage(getSampleProject(), org, name, version).map(
                pkg -> pkg.getCompilation().getSemanticModel(pkg.getDefaultModule().moduleId()));
    }

    public static Optional<SemanticModel> getSemanticModel(String org, String name) {
        return getModulePackage(getSampleProject(), org, name).map(
                pkg -> pkg.getCompilation().getSemanticModel(pkg.getDefaultModule().moduleId()));
    }

    /**
     * Retrieves a package matching the specified organization, name, and version. If the package is not found in the
     * local cache, it attempts to fetch it from the remote repository.
     *
     * @param buildProject The build project context
     * @param org          The organization name of the package
     * @param name         The name of the package
     * @param version      The version of the package
     * @return An Optional containing the matching Package if found, empty Optional otherwise
     */
    public static Optional<Package> getModulePackage(BuildProject buildProject, String org, String name,
                                                     String version) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)));

        Collection<ResolutionResponse> resolutionResponses =
                buildProject.projectEnvironmentContext().getService(PackageResolver.class)
                        .resolvePackages(Collections.singletonList(resolutionRequest),
                                ResolutionOptions.builder().setOffline(false).build());
        Optional<ResolutionResponse> resolutionResponse = resolutionResponses.stream().findFirst();
        if (resolutionResponse.isEmpty()) {
            return Optional.empty();
        }

        Path balaPath = resolutionResponse.get().resolvedPackage().project().sourceRoot();
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        BalaProject balaProject = BalaProject.loadProject(defaultBuilder, balaPath);
        return Optional.ofNullable(balaProject.currentPackage());
    }

    public static Optional<Package> getModulePackage(BuildProject buildProject, String org, String name) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name)));
        PackageResolver packageResolver = buildProject.projectEnvironmentContext().getService(PackageResolver.class);
        Collection<PackageMetadataResponse> packageMetadataResponses = packageResolver.resolvePackageMetadata(
                Collections.singletonList(resolutionRequest),
                ResolutionOptions.builder().setOffline(true).build());
        Optional<PackageMetadataResponse> pkgMetadata = packageMetadataResponses.stream().findFirst();
        if (pkgMetadata.isEmpty()) {
            return Optional.empty();
        }

        Collection<ResolutionResponse> resolutionResponses = packageResolver.resolvePackages(
                Collections.singletonList(ResolutionRequest.from(pkgMetadata.get().resolvedDescriptor())),
                ResolutionOptions.builder().setOffline(false).build());
        Optional<ResolutionResponse> resolutionResponse = resolutionResponses.stream().findFirst();
        if (resolutionResponse.isEmpty()) {
            return Optional.empty();
        }

        Path balaPath = resolutionResponse.get().resolvedPackage().project().sourceRoot();
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        BalaProject balaProject = BalaProject.loadProject(defaultBuilder, balaPath);
        return Optional.ofNullable(balaProject.currentPackage());
    }

    public static boolean isModuleUnresolved(String org, String name, String version) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)));
        PackageResolver packageResolver = SAMPLE_PROJECT.projectEnvironmentContext().getService(PackageResolver.class);
        return packageResolver.resolvePackageMetadata(Collections.singletonList(resolutionRequest),
                        ResolutionOptions.builder().setOffline(true).build()).stream()
                .findFirst()
                .map(response -> response.resolutionStatus() == ResolutionResponse.ResolutionStatus.UNRESOLVED)
                .orElse(false);
    }

    private static Path getPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null");
    }

    private static Path getParentPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null").getParent();
    }

    /**
     * Load the project from the given file path.
     *
     * @param workspaceManager the workspace manager
     * @param filePath         the file path
     * @return the loaded project
     */
    public static Project loadProject(WorkspaceManager workspaceManager, Path filePath) {
        try {
            return workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Error loading project: " + e.getMessage());
        }
    }

    /**
     * Retrieves the semantic model of the default module of a package if the package details match the provided
     * organization, package name, and version.
     *
     * @param workspaceManager the workspace manager used to load the project
     * @param filePath         the path to the file from which the project should be loaded
     * @param orgName          the organization name that must match the package descriptor's organization value
     * @param packageName      the package name that must match the package descriptor's name value
     * @param modulePartName   the module part name that must match the package descriptor's submodule part name value
     * @param version          the version that must match the package descriptor's version value
     * @return an Optional containing the semantic model
     */
    public static Optional<SemanticModel> getSemanticModelIfMatched(WorkspaceManager workspaceManager, Path filePath,
                                                                    String orgName, String packageName,
                                                                    String modulePartName,
                                                                    String version) {
        try {
            Project project = workspaceManager.loadProject(filePath);
            Package currentPackage = project.currentPackage();
            PackageDescriptor descriptor = currentPackage.descriptor();
            if (descriptor.org().value().equals(orgName) &&
                    descriptor.name().value().equals(packageName) &&
                    descriptor.version().value().toString().equals(version)) {
                ModuleId moduleId = currentPackage.getDefaultModule().moduleId();
                if (Objects.nonNull(modulePartName) && !modulePartName.isEmpty()) {
                    ModuleName subModuleName = ModuleName.from(PackageName.from(packageName), modulePartName);
                    Module module = currentPackage.module(subModuleName);
                    if (module == null) {
                        return Optional.empty();
                    }
                    moduleId = module.moduleId();
                }
                return Optional.of(currentPackage
                        .getCompilation()
                        .getSemanticModel(moduleId));
            }
        } catch (WorkspaceDocumentException | EventSyncException e) {
        }
        return Optional.empty();
    }

    public static ModuleInfo fetchVersionIfNotExists(ModuleInfo moduleInfo) {
        if (moduleInfo.version() == null) {
            CentralAPI centralApi = RemoteCentral.getInstance();
            return new ModuleInfo(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.moduleName(),
                    centralApi.latestPackageVersion(moduleInfo.org(), moduleInfo.packageName()));
        }
        return moduleInfo;
    }

    public static Optional<Package> pullModuleAndNotify(LSClientLogger lsClientLogger, ModuleInfo moduleInfo) {
        ModuleInfo completeModuleInfo = fetchVersionIfNotExists(moduleInfo);
        Optional<Package> modulePackage;
        if (PackageUtil.isModuleUnresolved(completeModuleInfo.org(), completeModuleInfo.packageName(),
                completeModuleInfo.version())) {
            notifyClient(lsClientLogger, completeModuleInfo, MessageType.Info, PULLING_THE_MODULE_MESSAGE);
            modulePackage = getModulePackage(SAMPLE_PROJECT, completeModuleInfo.org(), completeModuleInfo.packageName(),
                    completeModuleInfo.version());
            if (modulePackage.isEmpty()) {
                notifyClient(lsClientLogger, completeModuleInfo, MessageType.Error, MODULE_PULLING_FAILED_MESSAGE);
            } else {
                notifyClient(lsClientLogger, completeModuleInfo, MessageType.Info, MODULE_PULLING_SUCCESS_MESSAGE);
            }
        } else {
            modulePackage = getModulePackage(SAMPLE_PROJECT, completeModuleInfo.org(), completeModuleInfo.packageName(),
                    completeModuleInfo.version());
        }
        return modulePackage;
    }

    private static void notifyClient(LSClientLogger lsClientLogger, ModuleInfo moduleInfo, MessageType messageType,
                                     String message) {
        if (lsClientLogger != null) {
            String signature =
                    String.format("%s/%s:%s", moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version());
            lsClientLogger.notifyClient(messageType, String.format(message, signature));
        }
    }
}
