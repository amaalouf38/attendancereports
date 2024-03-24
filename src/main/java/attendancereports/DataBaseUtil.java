package attendancereports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataBaseUtil {

    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/stc2fitforlife";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "root";

    public static List<String[]> executeSelectQuery(String sql, List<Object> args) throws SQLException {
        List<String[]> results = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < args.size(); i++) {
                statement.setObject(i + 1, args.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    String[] row = new String[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        row[i - 1] = resultSet.getString(i);
                    }
                    results.add(row);
                }
            }

            return results;
        }
    }

    public static int executeUpdate(String sql, List<Object> args) throws SQLException {
        int rowsAffected = 0;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.size(); i++) {
                statement.setObject(i + 1, args.get(i));
            }
            rowsAffected = statement.executeUpdate();
        }

        return rowsAffected;
    }

    private static String loadresourceasstring(String resourceName) throws IOException {
        try (InputStream inputStream = DataBaseUtil.class.getClassLoader().getResourceAsStream(resourceName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static void generateReport(String query, List<Object> parameters, String outputPath_, List<String[]> headers)
            throws IOException, SQLException {
        String sql = DataBaseUtil.loadresourceasstring(query);
        List<String[]> results = DataBaseUtil.executeSelectQuery(sql, parameters);
        List<String[]> report = new ArrayList<>(headers);
        report.addAll(results);
        Utility.writeCsv(outputPath_, report);
    }

}
