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

package io.ballerina.indexgenerator;

import io.ballerina.compiler.api.symbols.ParameterKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:central-index.sqlite";
    private static final String CENTRAL_INDEX_SQL =
            "/Users/nipunaf/projects/ballerina/ballerina-dev-tools/flow-model-generator/modules/flow-model-index" +
                    "-generator/src/main/resources/central-index.sql";

    public static void createDatabase() {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(CENTRAL_INDEX_SQL)));
            executeQuery(sql);
        } catch (IOException e) {
            Logger.getGlobal().severe("Error reading SQL file: " + e.getMessage());
        }
    }

    public static int insertPackage(String org, String name, String version) {
        String sql = "INSERT INTO Package (org, name, version) VALUES ('" + org + "', '" + name + "', '" +
                version + "')";
        return executeInsertQuery(sql);
    }

    public static int executeInsertQuery(String sql) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating package failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return -1;
        }
    }

    private static void executeQuery(String sql) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Query executed successfully.");
            stmt.getGeneratedKeys();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
        }
    }

    public static int insertFunction(int packageId, String s, String description, String returnType) {
        String sql = String.format(
                "INSERT INTO Function (package_id, name, description, return_type) VALUES ('%d', '%s', '%s', '%s')",
                packageId, s, description, returnType);
        return executeInsertQuery(sql);
    }

    public static void insertFunctionParameter(int functionId, String paramName, String paramDescription,
                                               String paramType, ParameterKind parameterKind) {
        String sql = String.format(
                "INSERT INTO Parameter (function_id, name, description, type, kind) VALUES ('%d', '%s', '%s', '%s', " +
                        "'%s')",
                functionId, paramName, paramDescription, paramType, parameterKind.name());
        executeInsertQuery(sql);
    }
}