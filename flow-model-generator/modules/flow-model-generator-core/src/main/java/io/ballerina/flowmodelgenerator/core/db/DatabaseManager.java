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

package io.ballerina.flowmodelgenerator.core.db;

import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final String INDEX_FILE_NAME = "central-index.sqlite";
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private String dbPath;

    public DatabaseManager() {
        URL dbUrl = getClass().getClassLoader().getResource(INDEX_FILE_NAME);
        if (dbUrl == null) {
            throw new RuntimeException("Database resource not found: " + INDEX_FILE_NAME);
        }
        dbPath = "jdbc:sqlite:" + dbUrl.getPath();
    }

    public List<FunctionResult> getAllFunctions() {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "p.name AS package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = 'FUNCTION' " +
                "LIMIT 20;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            List<FunctionResult> functionResults = new ArrayList<>();
            while (rs.next()) {
                FunctionResult functionResult = new FunctionResult(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("org"),
                        rs.getString("version")
                );
                functionResults.add(functionResult);
            }
            return functionResults;
        } catch (SQLException e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public List<FunctionResult> searchFunctions(Map<String, String> queryMap) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "p.name AS package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = 'FUNCTION' " +
                "AND (" +
                "f.name LIKE ? OR " +
                "p.name LIKE ? " +
                ")" +
                "LIMIT ? " +
                "OFFSET ?;";
        String wildcardKeyword = "%" + queryMap.get("q") + "%";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, wildcardKeyword);
            stmt.setString(2, wildcardKeyword);
            stmt.setString(3, queryMap.get("limit"));
            stmt.setString(4, queryMap.get("offset"));
            ResultSet rs = stmt.executeQuery();
            List<FunctionResult> functionResults = new ArrayList<>();
            while (rs.next()) {
                FunctionResult functionResult = new FunctionResult(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("org"),
                        rs.getString("version")
                );
                functionResults.add(functionResult);
                System.out.println(rs.getString("function_name"));
            }
            return functionResults;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<FunctionResult> getFunction(String org, String module, String symbol) {
        String sql = "SELECT " +
                "f.function_id, " +
                "f.name AS function_name, " +
                "f.description AS function_description, " +
                "f.return_type, " +
                "p.name AS package_name, " +
                "p.org, " +
                "p.version " +
                "FROM Function f " +
                "JOIN Package p ON f.package_id = p.package_id " +
                "WHERE f.kind = 'FUNCTION' " +
                "AND p.org = ? " +
                "AND p.name = ? " +
                "AND f.name = ?;";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, org);
            stmt.setString(2, module);
            stmt.setString(3, symbol);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new FunctionResult(
                        rs.getInt("function_id"),
                        rs.getString("function_name"),
                        rs.getString("function_description"),
                        rs.getString("return_type"),
                        rs.getString("package_name"),
                        rs.getString("org"),
                        rs.getString("version")
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<ParameterResult> getFunctionParameters(int functionId) {
        String sql = "SELECT " +
                "p.parameter_id, " +
                "p.name, " +
                "p.type, " +
                "p.kind, " +
                "p.description " +
                "FROM Parameter p " +
                "WHERE p.function_id = ?;";
        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, functionId);
            ResultSet rs = stmt.executeQuery();
            List<ParameterResult> parameterResults = new ArrayList<>();
            while (rs.next()) {
                ParameterResult parameterResult = new ParameterResult(
                        rs.getInt("parameter_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        ParameterKind.valueOf(rs.getString("kind")),
                        rs.getString("description")
                );
                parameterResults.add(parameterResult);
            }
            return parameterResults;
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return List.of();
        }
    }
}