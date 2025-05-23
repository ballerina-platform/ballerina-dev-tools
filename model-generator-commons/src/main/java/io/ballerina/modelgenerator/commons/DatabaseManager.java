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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages database operations for retrieving information about external connectors and functions.
 *
 * @since 2.0.0
 */
public class DatabaseManager {

    private static final String INDEX_FILE_NAME = "central-index.sqlite";
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private final String dbPath;

    private static class Holder {

        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("central-index");
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

    @Deprecated
    public List<FunctionData> getAllFunctions(FunctionData.Kind kind, Map<String, String> queryMap) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "f.resource_path, " +
                "f.kind, " +
                "f.return_error, " +
                "f.inferred_return_type, " +
                "f.import_statements, " +
                "p.name AS module_name, " +
                "p.package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = ? " +
                "LIMIT ? " +
                "OFFSET ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, kind.name());
            stmt.setString(2, queryMap.get("limit"));
            stmt.setString(3, queryMap.get("offset"));
            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    @Deprecated
    public List<FunctionData> getFunctionsByOrg(String orgName, FunctionData.Kind functionKind) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "f.kind, " +
                "f.return_error, " +
                "f.inferred_return_type, " +
                "f.import_statements, " +
                "f.resource_path, " +
                "p.name AS module_name, " +
                "p.package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = ? AND p.org = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, functionKind.name());
            stmt.setString(2, orgName);
            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    @Deprecated
    public List<FunctionData> searchFunctions(Map<String, String> queryMap, FunctionData.Kind kind) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "f.resource_path, " +
                "f.kind, " +
                "f.return_error, " +
                "f.inferred_return_type, " +
                "f.import_statements, " +
                "p.name AS module_name, " +
                "p.package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = ? " +
                "AND (" +
                "f.name LIKE ? OR " +
                "p.name LIKE ? " +
                ")" +
                "LIMIT ? " +
                "OFFSET ?;";
        String wildcardKeyword = "%" + queryMap.get("q") + "%";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, kind.name());
            stmt.setString(2, wildcardKeyword);
            stmt.setString(3, wildcardKeyword);
            stmt.setString(4, queryMap.get("limit"));
            stmt.setString(5, queryMap.get("offset"));
            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<FunctionData> getFunction(String org, String packageName, String moduleName, String symbol,
                                              FunctionData.Kind kind, String resourcePath) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append("f.function_id, ");
        sql.append("f.name AS function_name, ");
        sql.append("f.description AS function_description, ");
        sql.append("f.return_type, ");
        sql.append("f.resource_path, ");
        sql.append("f.kind, ");
        sql.append("f.return_error, ");
        sql.append("f.inferred_return_type, ");
        sql.append("f.import_statements, ");
        sql.append("p.module_name AS module_name, ");
        sql.append("p.package_name, ");
        sql.append("p.org, ");
        sql.append("p.version ");
        sql.append("FROM Function f ");
        sql.append("JOIN Package p ON f.package_id = p.package_id ");
        sql.append("WHERE p.org = ? ");
        sql.append("AND p.package_name = ? ");
        sql.append("AND p.module_name = ? ");
        sql.append("AND f.kind = ? ");
        sql.append("AND f.name = ? ");
        if (resourcePath != null) {
            sql.append("AND f.resource_path = ?");
        }

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, org);
            stmt.setString(2, packageName);
            stmt.setString(3, moduleName);
            stmt.setString(4, kind.name());
            stmt.setString(5, symbol);
            if (resourcePath != null) {
                stmt.setString(6, resourcePath);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Deprecated
    public Optional<FunctionData> getFunction(int functionId) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "p.name AS module_name, " +
                "p.package_name, " +
                "p.org, " +
                "p.version, " +
                "f.resource_path, " +
                "f.kind, " +
                "f.return_error, " +
                "f.inferred_return_type " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.function_id = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, functionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Deprecated
    public List<ParameterData> getFunctionParameters(int functionId) {
        String sql = "SELECT " +
                "p.parameter_id, " +
                "p.name, " +
                "p.type, " +
                "p.kind, " +
                "p.optional, " +
                "p.default_value, " +
                "p.description, " +
                "p.import_statements " +
                "FROM Parameter p " +
                "WHERE p.function_id = ?;";
        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, functionId);
            ResultSet rs = stmt.executeQuery();
            List<ParameterData> parameterResults = new ArrayList<>();
            while (rs.next()) {
                ParameterData
                        parameterData = new ParameterData(
                        rs.getInt("parameter_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        ParameterData.Kind.valueOf(rs.getString("kind")),
                        rs.getString("default_value"),
                        rs.getString("description"),
                        "",
                        rs.getBoolean("optional"),
                        rs.getString("import_statements"),
                        new ArrayList<>()
                );
                parameterResults.add(parameterData);
            }
            return parameterResults;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public LinkedHashMap<String, ParameterData> getFunctionParametersAsMap(int functionId) {
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
                "pmt.package_identifier AS member_package_identifier, " +
                "pmt.package_name AS member_package_name " +
                "FROM Parameter p " +
                "LEFT JOIN ParameterMemberType pmt ON p.parameter_id = pmt.parameter_id " +
                "WHERE p.function_id = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, functionId);
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
                String memberPackageIdentifier = rs.getString("member_package_identifier");
                String memberPackageName = rs.getString("member_package_name");

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
                            memberType, memberKind, memberPackageIdentifier, memberPackageName);
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
                    null,
                    optional,
                    importStatements,
                    typeMembers
            );
        }
    }

    @Deprecated
    public List<FunctionData> getConnectorActions(int connectorId) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description, " +
                "f.kind, " +
                "f.return_type, " +
                "f.resource_path, " +
                "f.return_error, " +
                "f.inferred_return_type " +
                "FROM Function f " +
                "JOIN FunctionConnector fc ON f.function_id = fc.function_id " +
                "WHERE fc.connector_id = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, connectorId);
            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("description"),
                        rs.getString("return_type"),
                        null, // packageName is not selected in this query
                        null, // moduleName is not selected in this query
                        null, // org is not selected in this query
                        null, // version is not selected in this query
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public List<FunctionData> getMethods(String connectorName, String org, String moduleName) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description, " +
                "f.kind, " +
                "p.version, " +
                "p.package_name, " +
                "f.return_type, " +
                "f.resource_path, " +
                "f.return_error, " +
                "f.inferred_return_type, " +
                "f.import_statements " +
                "FROM Function f " +
                "JOIN FunctionConnector fc ON f.function_id = fc.function_id " +
                "JOIN Function c ON fc.connector_id = c.function_id " +
                "JOIN Package p ON c.package_id = p.package_id " +
                "WHERE c.name = ? " +
                "AND p.org = ? " +
                "AND p.name = ? " +
                "AND c.kind = 'CONNECTOR';";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, connectorName);
            stmt.setString(2, org);
            stmt.setString(3, moduleName);
            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        moduleName,
                        org,
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    @Deprecated
    public List<FunctionData> searchFunctionsInPackages(List<String> packageNames, Map<String, String> queryMap,
                                                        FunctionData.Kind kind) {
        if (packageNames == null || packageNames.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append("f.function_id, ");
        sql.append("f.name AS function_name, ");
        sql.append("f.description AS function_description, ");
        sql.append("f.return_type, ");
        sql.append("f.resource_path, ");
        sql.append("f.kind, ");
        sql.append("f.return_error, ");
        sql.append("f.inferred_return_type, ");
        sql.append("f.import_statements, ");
        sql.append("p.name AS module_name, ");
        sql.append("p.package_name, ");
        sql.append("p.org, ");
        sql.append("p.version ");
        sql.append("FROM Function f ");
        sql.append("JOIN Package p ON f.package_id = p.package_id ");
        sql.append("WHERE p.name IN (");
        for (int i = 0; i < packageNames.size(); i++) {
            sql.append("?");
            if (i < packageNames.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(") AND f.kind = ? ");

        boolean hasQuery = queryMap.containsKey("q");
        if (hasQuery) {
            sql.append("AND (f.name LIKE ? OR p.name LIKE ?) ");
        }

        sql.append("LIMIT ? OFFSET ?");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            // Set package name parameters
            for (String packageName : packageNames) {
                stmt.setString(paramIndex++, packageName);
            }

            // Set function kind parameter
            stmt.setString(paramIndex++, kind.name());

            // Set wildcard parameters if search query exists
            if (hasQuery) {
                String wildcardKeyword = "%" + queryMap.get("q") + "%";
                stmt.setString(paramIndex++, wildcardKeyword);
                stmt.setString(paramIndex++, wildcardKeyword);
            }

            // Set limit and offset with defaults if not provided
            stmt.setInt(paramIndex++, queryMap.containsKey("limit") ? Integer.parseInt(queryMap.get("limit")) : 10);
            stmt.setInt(paramIndex, queryMap.containsKey("offset") ? Integer.parseInt(queryMap.get("offset")) : 0);

            ResultSet rs = stmt.executeQuery();
            List<FunctionData> functionDataList = new ArrayList<>();
            while (rs.next()) {
                FunctionData functionData = new FunctionData(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("module_name"),
                        rs.getString("org"),
                        rs.getString("version"),
                        rs.getString("resource_path"),
                        FunctionData.Kind.valueOf(rs.getString("kind")),
                        rs.getBoolean("return_error"),
                        rs.getBoolean("inferred_return_type"),
                        rs.getString("import_statements"));
                functionDataList.add(functionData);
            }
            return functionDataList;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }
}
