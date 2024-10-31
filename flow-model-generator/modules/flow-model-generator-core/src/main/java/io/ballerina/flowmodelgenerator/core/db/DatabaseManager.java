package io.ballerina.flowmodelgenerator.core.db;

import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    public List<FunctionResult> searchFunctions(String keyword) {
        String sql = "SELECT " +
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
                ");";

        String wildcardKeyword = "%" + keyword + "%";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, wildcardKeyword);
            stmt.setString(2, wildcardKeyword);
            ResultSet rs = stmt.executeQuery();
            List<FunctionResult> functionResults = new ArrayList<>();
            while (rs.next()) {
                FunctionResult functionResult = new FunctionResult(
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
}