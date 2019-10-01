package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String loadUrlsFromDatabase() throws SQLException;

    String getNextLinkThenDel();

    void storeNewsIntoDatabase(String link, String title, String content);

    void deleteFromDatabase(String link);

    void addLinkIntoDatase(String link, String table);

    Boolean isProcessedLink(String link);
}
