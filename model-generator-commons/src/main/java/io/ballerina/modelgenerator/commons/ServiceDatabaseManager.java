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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages database operations for retrieving information about external connectors and functions.
 *
 * @since 2.0.0
 */
public class ServiceDatabaseManager {

    private static final String INDEX_FILE_NAME = "service-index.sqlite";
    private static final Logger LOGGER = Logger.getLogger(ServiceDatabaseManager.class.getName());
    private final String dbPath;

    private static class Holder {

        private static final ServiceDatabaseManager INSTANCE = new ServiceDatabaseManager();
    }

    public static ServiceDatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    private ServiceDatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("service-index");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a temporary directory", e);
        }

        URL dbUrl = getClass().getClassLoader().getResource(INDEX_FILE_NAME);
        if (dbUrl == null) {
            throw new RuntimeException("Database resource not found: " + INDEX_FILE_NAME);
        }
        Path tempFile = tempDir.resolve(INDEX_FILE_NAME);
        try {
            Files.copy(dbUrl.openStream(), tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy the database file to the temporary directory", e);
        }

        dbPath = "jdbc:sqlite:" + tempFile.toString();
    }

    public Optional<FunctionData> getListener(String module) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append("l.listener_id, ");
        sql.append("l.name AS listener_name, ");
        sql.append("l.description AS listener_description, ");
        sql.append("l.return_error, ");
        sql.append("p.package_id, ");
        sql.append("p.name AS package_name, ");
        sql.append("p.org, ");
        sql.append("p.version ");
        sql.append("FROM Listener l ");
        sql.append("JOIN Package p ON l.package_id = p.package_id ");
        sql.append("WHERE p.name = ? ");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, module);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("listener_id"),
                        rs.getString("listener_name"),
                        rs.getString("listener_description"),
                        null,
                        rs.getString("package_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        null,
                        null,
                        rs.getBoolean("return_error"),
                        false,
                        null);
                functionData.setPackageId(rs.getString("package_id"));
                return Optional.of(functionData);
            }
            return Optional.empty();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return Optional.empty();
        }
    }

    public LinkedHashMap<String, ParameterData> getFunctionParametersAsMap(int listenerId) {
        String sql = "SELECT " +
                "p.parameter_id, " +
                "p.name, " +
                "p.type, " +
                "p.kind, " +
                "p.optional, " +
                "p.default_value, " +
                "p.description, " +
                "p.import_statements, " +
                "pmt.type AS member_type, " +
                "pmt.kind AS member_kind, " +
                "pmt.package AS member_package " +
                "FROM Parameter p " +
                "LEFT JOIN ParameterMemberType pmt ON p.parameter_id = pmt.parameter_id " +
                "WHERE p.listener_id = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listenerId);
            ResultSet rs = stmt.executeQuery();

            // Use a builder to accumulate parameter data and member types
            LinkedHashMap<String, ParameterDataBuilder> builders = new LinkedHashMap<>();

            while (rs.next()) {
                String paramName = rs.getString("name");
                int parameterId = rs.getInt("parameter_id");
                String type = rs.getString("type");
                ParameterData.Kind kind = ParameterData.Kind.valueOf(rs.getString("kind"));
                String defaultValue = rs.getString("default_value");
                String description = rs.getString("description");
                boolean optional = rs.getBoolean("optional");
                String importStatements = rs.getString("import_statements");

                // Member type data
                String memberType = rs.getString("member_type");
                String memberKind = rs.getString("member_kind");
                String memberPackage = rs.getString("member_package");

                // Get or create the builder for this parameter
                ParameterDataBuilder builder = builders.get(paramName);
                if (builder == null) {
                    builder = new ParameterDataBuilder();
                    builder.parameterId = parameterId;
                    builder.name = paramName;
                    builder.type = type;
                    builder.kind = kind;
                    builder.defaultValue = defaultValue;
                    builder.description = description;
                    builder.optional = optional;
                    builder.importStatements = importStatements;
                    builders.put(paramName, builder);
                }

                // Add member type if present
                if (memberType != null) {
                    ParameterMemberTypeData memberData = new ParameterMemberTypeData(
                            memberType, memberKind, memberPackage);
                    builder.typeMembers.add(memberData);
                }
            }

            // Convert builders to ParameterData
            LinkedHashMap<String, ParameterData> parameterResults = new LinkedHashMap<>();
            for (ParameterDataBuilder builder : builders.values()) {
                parameterResults.put(builder.name, builder.build());
            }
            return parameterResults;

        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public Optional<ServiceDeclaration> getServiceDeclaration(String moduleName) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append("s.display_name, ");
        sql.append("s.optional_type_descriptor, ");
        sql.append("s.type_descriptor_label, ");
        sql.append("s.type_descriptor_description, ");
        sql.append("s.type_descriptor_default_value, ");
        sql.append("s.add_default_type_descriptor, ");
        sql.append("s.optional_absolute_resource_path, ");
        sql.append("s.absolute_resource_path_label, ");
        sql.append("s.absolute_resource_path_description, ");
        sql.append("s.absolute_resource_path_default_value, ");
        sql.append("s.optional_string_literal, ");
        sql.append("s.string_literal_label, ");
        sql.append("s.string_literal_description, ");
        sql.append("s.string_literal_default_value, ");
        sql.append("s.listener_kind, ");
        sql.append("s.kind, ");
        sql.append("p.package_id, ");
        sql.append("p.org, ");
        sql.append("p.name AS package_name, ");
        sql.append("p.version ");
        sql.append("FROM ServiceDeclaration s ");
        sql.append("JOIN Package p ON s.package_id = p.package_id ");
        sql.append("WHERE p.name = ?");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, moduleName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ServiceDeclaration.Package packageInfo = new ServiceDeclaration.Package(
                        rs.getInt("package_id"),
                        rs.getString("org"),
                        rs.getString("package_name"),
                        rs.getString("version")
                );

                ServiceDeclaration serviceDeclaration = new ServiceDeclaration(
                        packageInfo,
                        rs.getString("display_name"),
                        rs.getInt("optional_type_descriptor"),
                        rs.getString("type_descriptor_label"),
                        rs.getString("type_descriptor_description"),
                        rs.getString("type_descriptor_default_value"),
                        rs.getInt("add_default_type_descriptor"),
                        rs.getInt("optional_absolute_resource_path"),
                        rs.getString("absolute_resource_path_label"),
                        rs.getString("absolute_resource_path_description"),
                        rs.getString("absolute_resource_path_default_value"),
                        rs.getInt("optional_string_literal"),
                        rs.getString("string_literal_label"),
                        rs.getString("string_literal_description"),
                        rs.getString("string_literal_default_value"),
                        rs.getString("listener_kind"),
                        rs.getString("kind")
                );

                return Optional.of(serviceDeclaration);
            }
            return Optional.empty();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<String> getServiceTypes(int packageId) {
        String sql = "SELECT DISTINCT name FROM ServiceType WHERE package_id = ?";
        List<String> serviceTypes = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, packageId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                serviceTypes.add(rs.getString("name"));
            }
            return serviceTypes;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public List<ServiceTypeFunction> getMatchingServiceTypeFunctions(int packageId, String serviceType) {

        String sql = "SELECT " +
                "f.function_id, " +
                "f.name, " +
                "f.description, " +
                "f.accessor, " +
                "f.kind, " +
                "f.return_type, " +
                "f.return_type_editable, " +
                "f.import_statements, " +
                "f.enable " +
                "FROM ServiceTypeFunction f " +
                "JOIN ServiceType st ON f.service_type_id = st.service_type_id " +
                "WHERE st.package_id = ? AND st.name = ?";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, packageId);
            stmt.setString(2, serviceType);

            ResultSet rs = stmt.executeQuery();
            List<ServiceTypeFunction> functions = new ArrayList<>();
            while (rs.next()) {
                int functionId = rs.getInt("function_id");
                List<ServiceTypeFunction.ServiceTypeFunctionParameter> params = getServiceFunctionParams(functionId);
                functions.add(new ServiceTypeFunction(
                        rs.getInt("function_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("accessor"),
                        rs.getString("kind"),
                        rs.getString("return_type"),
                        rs.getInt("return_type_editable"),
                        rs.getString("import_statements"),
                        rs.getInt("enable"),
                        params
                ));
            }
            return functions;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    private List<ServiceTypeFunction.ServiceTypeFunctionParameter> getServiceFunctionParams(int functionId) {
        String sql = "SELECT " +
                "parameter_id, " +
                "name, " +
                "label, " +
                "description, " +
                "kind, " +
                "type, " +
                "default_value, " +
                "import_statements " +
                "FROM ServiceTypeFunctionParameter " +
                "WHERE function_id = ?";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, functionId);

            ResultSet rs = stmt.executeQuery();
            List<ServiceTypeFunction.ServiceTypeFunctionParameter> parameters = new ArrayList<>();
            while (rs.next()) {
                parameters.add(new ServiceTypeFunction.ServiceTypeFunctionParameter(
                        rs.getInt("parameter_id"),
                        rs.getString("name"),
                        rs.getString("label"),
                        rs.getString("description"),
                        rs.getString("kind"),
                        rs.getString("type"),
                        rs.getString("default_value"),
                        rs.getString("import_statements")
                ));
            }
            return parameters;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    // Helper builder class
    private static class ParameterDataBuilder {

        int parameterId;
        String name;
        String type;
        ParameterData.Kind kind;
        String defaultValue;
        String description;
        boolean optional;
        String importStatements;
        List<ParameterMemberTypeData> typeMembers = new ArrayList<>();

        ParameterData build() {
            return new ParameterData(
                    parameterId,
                    name,
                    type,
                    kind,
                    defaultValue,
                    description,
                    optional,
                    importStatements,
                    typeMembers
            );
        }
    }

}
