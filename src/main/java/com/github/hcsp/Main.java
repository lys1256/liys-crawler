package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Main {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        //从数据库加载即将处理的链接的代码
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/news?serverTimezone=UTC", "root", "P@ssword-123");
        String link=null;
        while ((link=getNextLinkThenDel(connection))!=null) {
            //判断是否已经处理过连接
            if (isProcessedLink(connection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document document = httpGetAndParseHTML(link);
                parseUrlsFromPageAndSotreIntoDatabase(connection, document);
                storeIntoDatabaseIfItIsNews(connection,document,link);
                addLinkIntoDatase(connection, link, "insert into links_already_processed values(?)");
            }
        }


    }

    private static String getNextLinkThenDel(Connection connection) throws SQLException {
        List<String> resultSet=loadUrlsFromDatabase(connection, "select link from links_to_be_processed limit 1");
        String nextlink=resultSet.get(0);
        System.out.println(nextlink);
        if (nextlink==null) {
            return null;
        }
        deleteFromDatabase(connection,nextlink);
        return nextlink;
    }

    private static void parseUrlsFromPageAndSotreIntoDatabase(Connection connection, Document document) {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            if (href.toLowerCase().startsWith("javascript")){
                continue;
            }
                if (href.startsWith("//")) {
                    href.replaceFirst("//", href = "https:" + href);
                }
            addLinkIntoDatase(connection, href, "insert into links_to_be_processed values(?)");
        }
    }

    private static Boolean isProcessedLink(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("select Link FROM links_already_processed WHERE LINK = ?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static void addLinkIntoDatase(Connection connection, String link, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFromDatabase(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM links_to_be_processed WHERE LINK = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
            return results;
        }
    }

    private static void storeIntoDatabaseIfItIsNews(Connection connection,Document document,String link) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String content=document.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                String title = articleTags.get(0).child(0).text();
                try {
                    PreparedStatement statement=connection.prepareStatement("insert into news (url,title,context,created_at,MODIFIED_AT)values (?,?,?,now(),now())");
                    statement.setString(1,link);
                    statement.setString(2,title);
                    statement.setString(3,content);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHTML(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        System.out.println(link);
        HttpGet httpGet = new HttpGet(link);
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String HTML = EntityUtils.toString(entity1);
            Document document = Jsoup.parse(HTML);
            //提取所有<a标签>
            return document;
        } finally {
            response1.close();
        }
    }

    private static boolean isInterestingLink(String link) {
        if (!link.contains("news.sina.cn") && !("https://sina.cn".equals(link))) {
            //这是我们不感兴趣的内容
        } else if (!link.contains("https://passport.sina.cn") && !link.contains("hotnews.sina")) {
            return true;
        }
        return false;
    }
}
