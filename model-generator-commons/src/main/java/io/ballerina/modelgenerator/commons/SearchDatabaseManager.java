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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages SQLite database operations for searching functions in a package repository.
 *
 * <p>
 * This class follows the Singleton pattern and handles the initialization and querying of a SQLite database containing
 * package and function information.
 * </p>
 *
 * @since 2.0.0
 */
public class SearchDatabaseManager {

    private static final String INDEX_FILE_NAME = "search-index.sqlite";
    private static final Logger LOGGER = Logger.getLogger(SearchDatabaseManager.class.getName());
    private final String dbPath;

    private static class Holder {

        private static final SearchDatabaseManager INSTANCE = new SearchDatabaseManager();
    }

    public static SearchDatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    private SearchDatabaseManager() {
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

        dbPath = "jdbc:sqlite:" + tempFile;
    }

    /**
     * Searches for functions in the database based on the given query.
     *
     * @param q      the search query string
     * @param limit  the maximum number of results to return
     * @param offset the offset from which to start returning results
     * @return a list of search results matching the query
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchFunctions(String q, int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();
        String sql = """
                SELECT
                    f.id,
                    f.name AS function_name,
                    f.description AS function_description,
                    f.package_id,
                    p.name AS package_name,
                    p.org AS package_org,
                    p.version AS package_version,
                    fts.rank
                FROM FunctionFTS AS fts
                JOIN Function AS f ON fts.rowid = f.id
                JOIN Package AS p ON f.package_id = p.id
                WHERE fts.FunctionFTS MATCH ?
                ORDER BY fts.rank
                LIMIT ?
                OFFSET ?;
                """;

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sanitizeQuery(q) + "*");
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String functionName = rs.getString("function_name");
                    String description = rs.getString("function_description");
                    String packageName = rs.getString("package_name");
                    String org = rs.getString("package_org");
                    String version = rs.getString("package_version");
                    SearchResult result = SearchResult.from(org, packageName, version, functionName, description);
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching functions: " + e.getMessage());
            throw new RuntimeException("Failed to search functions", e);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid number format in query parameters: " + e.getMessage());
            throw new RuntimeException("Invalid limit or offset value", e);
        }

        return results;
    }

    /**
     * Searches for connectors in the database based on the given query.
     *
     * @param q      the search query string
     * @param limit  the maximum number of results to return
     * @param offset the offset from which to start returning results
     * @return a list of search results matching the query
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchConnectors(String q, int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();
        String sql = """
                SELECT
                    c.id,
                    c.name AS connector_name,
                    c.description AS connector_description,
                    c.package_id,
                    p.name AS package_name,
                    p.org AS package_org,
                    p.version AS package_version,
                    fts.rank
                FROM ConnectorFTS AS fts
                JOIN Connector AS c ON fts.rowid = c.id
                JOIN Package AS p ON c.package_id = p.id
                WHERE fts.ConnectorFTS MATCH ?
                ORDER BY fts.rank
                LIMIT ?
                OFFSET ?;
                """;

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sanitizeQuery(q) + "*");
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String connectorName = rs.getString("connector_name");
                    String description = rs.getString("connector_description");
                    String packageName = rs.getString("package_name");
                    String org = rs.getString("package_org");
                    String version = rs.getString("package_version");
                    SearchResult result = SearchResult.from(org, packageName, version, connectorName, description);
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching connectors: " + e.getMessage());
            throw new RuntimeException("Failed to search connectors", e);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid number format in query parameters: " + e.getMessage());
            throw new RuntimeException("Invalid limit or offset value", e);
        }

        return results;
    }

    /**
     * Searches for functions that match both the given package names and function names.
     *
     * @param packageNames  List of package names to search in
     * @param functionNames List of function names to search for
     * @param limit         The maximum number of results to return
     * @param offset        The number of results to skip
     * @return A list of search results matching the criteria
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchFunctionsByPackages(List<String> packageNames, List<String> functionNames,
                                                        int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("f.name AS function_name, ")
                .append("f.description AS function_description, ")
                .append("f.package_id, ")
                .append("p.name AS package_name, ")
                .append("p.org AS package_org, ")
                .append("p.version AS package_version ")
                .append("FROM Package p ")
                .append("JOIN Function f ON p.id = f.package_id");

        // Build the SQL query with IN clauses for both packages and functions
        boolean whereAdded = false;
        if (!packageNames.isEmpty()) {
            sqlBuilder.append(" WHERE p.name IN (")
                    .append(String.join(",", Collections.nCopies(packageNames.size(), "?")))
                    .append(")");
            whereAdded = true;
        }
        if (!functionNames.isEmpty()) {
            sqlBuilder.append(whereAdded ? " AND" : " WHERE")
                    .append(" f.name IN (")
                    .append(String.join(",", Collections.nCopies(functionNames.size(), "?")))
                    .append(")");
        }
        sqlBuilder.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            // Set parameters for package names
            int paramIndex = 1;
            for (String packageName : packageNames) {
                stmt.setString(paramIndex++, packageName);
            }

            // Set parameters for function names
            for (String functionName : functionNames) {
                stmt.setString(paramIndex++, functionName);
            }

            // Set limit and offset
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("function_name");
                    String description = rs.getString("function_description");
                    String org = rs.getString("package_org");
                    String pkgName = rs.getString("package_name");
                    String version = rs.getString("package_version");

                    SearchResult.Package packageInfo = new SearchResult.Package(org, pkgName, version);
                    results.add(SearchResult.from(packageInfo, name, description));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching functions: " + e.getMessage());
            throw new RuntimeException("Failed to search functions", e);
        }

        return results;
    }

    /**
     * Searches for connectors that match the given package names and connector names.
     *
     * @param packageConnectorMap List containing the package name and connector name
     * @param limit               The maximum number of results to return
     * @param offset              The number of results to skip
     * @return A list of search results matching the criteria
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchConnectorsByPackage(List<String> packageConnectorMap, int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("c.name AS connector_name, ")
                .append("c.description AS connector_description, ")
                .append("c.package_id, ")
                .append("p.name AS package_name, ")
                .append("p.org AS package_org, ")
                .append("p.version AS package_version ")
                .append("FROM Package p ")
                .append("JOIN Connector c ON p.id = c.package_id");

        // Build the SQL query with IN clauses for both packages and connectors
        if (!packageConnectorMap.isEmpty()) {
            sqlBuilder.append(" WHERE (");
            for (int i = 0; i < packageConnectorMap.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(" OR ");
                }
                sqlBuilder.append("(p.name = ? AND c.name = ?)");
            }
            sqlBuilder.append(")");
        }
        sqlBuilder.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            // Set parameters for package names and connector names
            int paramIndex = 1;
            for (String mapping : packageConnectorMap) {
                String[] mappingTuple = mapping.split(":");
                stmt.setString(paramIndex++, mappingTuple[0]);
                stmt.setString(paramIndex++, mappingTuple[1]);
            }

            // Set limit and offset
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("connector_name");
                    String description = rs.getString("connector_description");
                    String org = rs.getString("package_org");
                    String pkgName = rs.getString("package_name");
                    String version = rs.getString("package_version");

                    SearchResult.Package packageInfo = new SearchResult.Package(org, pkgName, version);
                    results.add(SearchResult.from(packageInfo, name, description));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching connectors: " + e.getMessage());
            throw new RuntimeException("Failed to search connectors", e);
        }

        return results;
    }

    /**
     * Searches for types in the database based on the given query.
     *
     * @param q      the search query string
     * @param limit  the maximum number of results to return
     * @param offset the offset from which to start returning results
     * @return a list of search results matching the query
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchTypes(String q, int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();
        String sql = """
                SELECT
                    t.id,
                    t.name AS type_name,
                    t.description AS type_description,
                    t.package_id,
                    p.name AS package_name,
                    p.org AS package_org,
                    p.version AS package_version,
                    fts.rank
                FROM TypeFTS AS fts
                JOIN Type AS t ON fts.rowid = t.id
                JOIN Package AS p ON t.package_id = p.id
                WHERE fts.TypeFTS MATCH ?
                ORDER BY fts.rank
                LIMIT ?
                OFFSET ?;
                """;

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sanitizeQuery(q) + "*");
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String typeName = rs.getString("type_name");
                    String description = rs.getString("type_description");
                    String packageName = rs.getString("package_name");
                    String org = rs.getString("package_org");
                    String version = rs.getString("package_version");
                    SearchResult result = SearchResult.from(org, packageName, version, typeName, description);
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching types: " + e.getMessage());
            throw new RuntimeException("Failed to search types", e);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid number format in query parameters: " + e.getMessage());
            throw new RuntimeException("Invalid limit or offset value", e);
        }

        return results;
    }

    /**
     * Searches for types that match the given package names.
     *
     * @param packageNames List of package names to search in
     * @param limit        The maximum number of results to return
     * @param offset       The number of results to skip
     * @return A list of search results matching the criteria
     * @throws RuntimeException if there is an error executing the search or if the limit or offset values are invalid
     */
    public List<SearchResult> searchTypesByPackages(List<String> packageNames, int limit, int offset) {
        List<SearchResult> results = new ArrayList<>();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ")
                .append("t.name AS type_name, ")
                .append("t.description AS type_description, ")
                .append("t.package_id, ")
                .append("p.name AS package_name, ")
                .append("p.org AS package_org, ")
                .append("p.version AS package_version ")
                .append("FROM Package p ")
                .append("JOIN Type t ON p.id = t.package_id");

        // Build the SQL query with IN clauses for packages
        sqlBuilder.append(" WHERE p.name IN (")
                .append(String.join(",", Collections.nCopies(packageNames.size(), "?")))
                .append(")");
        sqlBuilder.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            // Set parameters for package names
            int paramIndex = 1;
            for (String packageName : packageNames) {
                stmt.setString(paramIndex++, packageName);
            }

            // Set limit and offset
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("type_name");
                    String description = rs.getString("type_description");
                    String org = rs.getString("package_org");
                    String pkgName = rs.getString("package_name");
                    String version = rs.getString("package_version");

                    SearchResult.Package packageInfo = new SearchResult.Package(org, pkgName, version);
                    results.add(SearchResult.from(packageInfo, name, description));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching types: " + e.getMessage());
            throw new RuntimeException("Failed to search types", e);
        }

        return results;
    }

    private static String sanitizeQuery(String q) {
        if (q == null || q.trim().isEmpty()) {
            return "";
        }
        // Escape quotes and remove special SQLite FTS operators, and only allow alphanumeric characters and spaces
        return q.replace("\"", "\"\"")
                .replaceAll("(?i)(UNION|SELECT|FROM|OR|AND|WHERE|MATCH|NEAR|NOT)|[^a-zA-Z0-9\\s\"]", " ")
                .trim();
    }

}
