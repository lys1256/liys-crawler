package com.github.hcsp;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcCrawlerDao implements CrawlerDao {
    private final Connection connection;

    public JdbcCrawlerDao() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/news?serverTimezone=UTC", "root", "P@ssword-123");
    }

    public List<String> loadUrlsFromDatabase(String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
            return results;
        }
    }

    public void storeNewsIntoDatabase(String link, String title, String content) {
        try {
            PreparedStatement statement = connection.prepareStatement("insert into news (url,title,context,created_at,MODIFIED_AT)values (?,?,?,now(),now())");
            statement.setString(1, link);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFromDatabase(String link) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM links_to_be_processed WHERE LINK = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addLinkIntoDatase(String link, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean isProcessedLink(String link) {
        try (PreparedStatement statement = connection.prepareStatement("select Link FROM links_already_processed WHERE LINK = ?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
