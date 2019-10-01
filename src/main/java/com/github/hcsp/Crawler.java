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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Crawler extends Thread{
    CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }


    public void setDataBaseAccessObject(JdbcCrawlerDao dataBaseAccessObject) {
        this.dao = dataBaseAccessObject;
    }

    public void run(){
        //从数据库加载即将处理的链接的代码
        try {
            String link = null;
            while ((link = getNextLinkThenDel()) != null) {
                //判断是否已经处理过连接
                if (dao.isProcessedLink(link)) {
                    continue;
                }
                if (isInterestingLink(link)) {
                    Document document = httpGetAndParseHTML(link);
                    parseUrlsFromPageAndSotreIntoDatabase(document);
                    storeIntoDatabaseIfItIsNews(document, link);
                    dao.addLinkIntoDatase(link, "links_already_processed");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private String getNextLinkThenDel() throws SQLException {
        String nextlink=dao.getNextLinkThenDel();
        //System.out.println(i);
        System.out.println(nextlink);
        if (nextlink == null) {
            return null;
        }
        return nextlink;
    }

    private void parseUrlsFromPageAndSotreIntoDatabase(Document document) {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            if (href.toLowerCase().startsWith("javascript")) {
                continue;
            }
            if (href.startsWith("//")) {
                href.replaceFirst("//", href = "https:" + href);
            }
            dao.addLinkIntoDatase(href, "links_to_be_processed");
        }
    }

    private void storeIntoDatabaseIfItIsNews(Document document, String link) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = document.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.storeNewsIntoDatabase(link, title, content);
                System.out.println(title+content);
            }
        }
    }

    private Document httpGetAndParseHTML(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //System.out.println(link);
        HttpGet httpGet = new HttpGet(link);
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        try {
           // System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String HTML = EntityUtils.toString(entity1);
            Document document = Jsoup.parse(HTML);
            //提取所有<a标签>
            return document;
        } finally {
            response1.close();
        }
    }

    private boolean isInterestingLink(String link) {
        if (!link.contains("news.sina.cn") && !("https://sina.cn".equals(link))) {
            //这是我们不感兴趣的内容
        } else if (!link.contains("https://passport.sina.cn") && !link.contains("hotnews.sina")) {
            return true;
        }
        return false;
    }
}
