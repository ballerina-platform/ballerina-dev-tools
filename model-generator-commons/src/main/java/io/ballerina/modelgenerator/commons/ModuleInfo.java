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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.PackageDescriptor;

/**
 * Represents information about a module.
 *
 * @param org         The organization name.
 * @param packageName The package name.
 * @param moduleName  The module name.
 * @param version     The version of the module.
 */
public record ModuleInfo(String org, String packageName, String moduleName, String version) {

    public static ModuleInfo from(ModuleID moduleId) {
        return new ModuleInfo(moduleId.orgName(), moduleId.packageName(), moduleId.moduleName(), moduleId.version());
    }

    public static ModuleInfo from(ModuleDescriptor moduleDescriptor) {
        return new ModuleInfo(moduleDescriptor.org().value(), moduleDescriptor.packageName().value(),
                moduleDescriptor.name().toString(), moduleDescriptor.version().value().toString());
    }

    public static ModuleInfo from(PackageDescriptor packageDescriptor) {
        String packageName = packageDescriptor.name().value();
        return new ModuleInfo(packageDescriptor.org().value(), packageName, packageName,
                packageDescriptor.version().value().toString());
    }

    public static ModuleInfo from(String moduleId) {
        String[] parts = moduleId.split(":");
        String version = parts.length != 2 ? null : parts[1];
        String[] packagePath = parts[0].split("/");
        String org = packagePath.length != 2 ? null : packagePath[0];
        String packageName = packagePath[1];
        return new ModuleInfo(org, packageName, packageName, version);
    }

    public boolean isComplete() {
        return org != null && packageName != null && moduleName != null && version != null;
    }
}
