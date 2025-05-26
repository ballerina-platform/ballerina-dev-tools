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
 * Manages SQLite database operations for storing and indexing Ballerina package information.
 * This class provides functionality to create and populate a search index database with
 * package-related data including functions, connectors, and types.
 * 
 * @since 2.0.0
 */
class SearchDatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String INDEX_FILE_NAME = "search-index.sqlite";
    private static final String CENTRAL_INDEX_SQL = "search-index.sql";
    private static final String dbPath = getDatabasePath();

    private static String getDatabasePath() {
        String destinationPath = Path
                .of("flow-model-generator/modules/flow-model-generator-ls-extension/src/main/resources")
                .resolve(INDEX_FILE_NAME)
                .toString();
        return "jdbc:sqlite:" + destinationPath;
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
        Path destinationPath = Path.of("flow-model-generator/modules/flow-model-index-generator/src/main/resources")
                .resolve(CENTRAL_INDEX_SQL);
        try {
            String sql = Files.readString(destinationPath);
            executeQuery(sql);
        } catch (IOException e) {
            LOGGER.severe("Error reading SQL file: " + e.getMessage());
        }
    }

    public static int insertPackage(String org, String name, String packageName, String version,
                                    int pullCount, List<String> keywords) {
        String sql = "INSERT INTO Package (org, name, package_name, version, pull_count, keywords) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        return insertEntry(sql,
                new Object[]{org, name, packageName, version, pullCount, keywords == null ? "" :
                        String.join(",", keywords)});
    }

    public static void insertFunction(String name, String description, int packageId) {
        String sql = "INSERT INTO Function (name, description, package_id) VALUES (?, ?, ?)";
        insertEntry(sql, new Object[]{name, description, packageId});
    }

    public static void insertConnector(String name, String description, String category, int packageId) {
        String sql = "INSERT INTO Connector (name, description, category, package_id) VALUES (?, ?, ?, ?)";
        insertEntry(sql, new Object[]{name, description, category, packageId});
    }

    public static void insertType(String name, String description, String kind, int packageId) {
        String sql = "INSERT INTO Type (name, description, kind, package_id) VALUES (?, ?, ?, ?)";
        insertEntry(sql, new Object[]{name, description, kind, packageId});
    }

    public static void deleteConnector(String packageName, List<String> connectors) {
        String sql = "DELETE FROM Connector WHERE package_id = (SELECT id FROM Package WHERE name = ?) AND name = ?";
        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String connector : connectors) {
                stmt.setString(1, packageName);
                stmt.setString(2, connector);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.severe("Error deleting connector: " + e.getMessage());
        }
    }
}
