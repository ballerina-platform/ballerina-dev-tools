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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

/**
 * Database Manager use to create the database and insert data.
 *
 * @since 2.0.0
 */
class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String INDEX_FILE_NAME = "service-index.sqlite";
    private static final String SERVICE_INDEX_SQL = "service-index.sql";
    private static final String dbPath = getDatabasePath();

    private static String getDatabasePath() {
        String destinationPath =
                Path.of("service-model-generator/modules/service-model-generator-ls-extension/src/main/resources")
                        .resolve(INDEX_FILE_NAME)
                        .toString();
        return "jdbc:sqlite:" + destinationPath;
    }

    private static int insertEntry(String sql, Object[] params) {
        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating package failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return -1;
        }
    }

    public static void createDatabase() {
        Path destinationPath =
                Path.of("service-model-generator/modules/service-model-index-generator/src/main/resources")
                        .resolve(SERVICE_INDEX_SQL);
        try {
            String sql = Files.readString(destinationPath);
            executeQuery(sql);
        } catch (IOException e) {
            LOGGER.severe("Error reading SQL file: " + e.getMessage());
        }
    }

    private static void executeQuery(String sql) {
        try (Connection conn = DriverManager.getConnection(dbPath);
             Statement stmt = conn.createStatement()) { // Use Statement instead
            stmt.executeUpdate(sql);
            LOGGER.info("Database created successfully");
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
        }
    }

    public static int insertPackage(String org, String name, String version, List<String> keywords) {
        String sql = "INSERT INTO Package (org, name, version, keywords) VALUES (?, ?, ?, ?)";
        return insertEntry(sql, new Object[]{org, name, version, keywords == null ? "" : String.join(",", keywords)});
    }

    public static int insertListener(int packageId, String name, String description, int returnError) {
        String sql = "INSERT INTO Listener (package_id, name, description, return_error) VALUES (?, ?, ?, ?)";
        return insertEntry(sql, new Object[]{packageId, name, description, returnError});
    }

    public static int insertListenerParameter(int listenerId, String paramName, String paramDescription,
                                              String paramType, String defaultValue,
                                              ServiceIndexGenerator.FunctionParameterKind parameterKind,
                                              int optional, String importStatements) {
        String sql =
                "INSERT INTO Parameter (listener_id, name, description, type, default_value, kind, optional, " +
                        "import_statements) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return insertEntry(sql,
                new Object[]{listenerId, paramName, paramDescription, paramType, defaultValue,
                        parameterKind.name(), optional, importStatements});
    }

    public static void insertParameterMemberType(int parameterId, String type, String kind, String packageIdentifier) {
        String sql = "INSERT INTO ParameterMemberType (parameter_id, type, kind, package) " +
                "VALUES (?, ?, ?, ?)";
        insertEntry(sql, new Object[]{parameterId, type, kind, packageIdentifier});
    }

}
