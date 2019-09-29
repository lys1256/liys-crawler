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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> linkPool = new ArrayList<>();
        linkPool.add("https://sina.cn");
        Set<String> processedLinks = new HashSet<>();
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            //提取连接
            String link = linkPool.remove(linkPool.size() - 1);
            //判断是否已经处理过连接
            if (processedLinks.contains(link)) {
                continue;
            }
            //重勾为自解释的函数
            //这是我们感兴趣的
            if (isInterestingLink(link)) {
                Document document = httpGetAndParseHTML(link);
                ArrayList<Element> links = document.select("a");
                links.stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
//              for (Element aTag:links){
//                  linkPool.add(aTag.attr("href"));
//              }
                //新闻详情处理
                storeIntoDatabaseIfItIsNews(document);
                processedLinks.add(link);
            }
        }


    }

    private static void storeIntoDatabaseIfItIsNews(Document document) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHTML(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link.replaceFirst("//", link = "https:" + link);
        }
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
        if ((!link.contains("news.sina.cn") && !"https://sina.cn".equals(link))) {
            //这是我们不感兴趣的内容
        } else if (!link.contains("https://passport.sina.cn") && !link.contains("hotnews.sina")) {
            return true;
        }
        return false;
    }
}
