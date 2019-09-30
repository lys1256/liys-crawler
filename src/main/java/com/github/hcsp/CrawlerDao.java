package com.github.hcsp;

import java.sql.SQLException;
import java.util.List;

public interface CrawlerDao {
    List<String> loadUrlsFromDatabase(String sql) throws SQLException;

    void storeNewsIntoDatabase(String link, String title, String content);

    void deleteFromDatabase(String link);

    void addLinkIntoDatase(String link, String sql);

    Boolean isProcessedLink(String link);
}
