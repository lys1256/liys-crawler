package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {
    SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() throws IOException {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public String loadUrlsFromDatabase() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            String a = (String) session.selectOne("com.github.hcsp.CrawlerMapper.selectNextAvailableLink");
            return a;
        }

    }

    @Override
    public synchronized String getNextLinkThenDel() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String a = (String) session.selectOne("com.github.hcsp.CrawlerMapper.selectNextAvailableLink");
            if (a!=null){
                session.delete("com.github.hcsp.CrawlerMapper.deletefromAvailableLink", a);
            }
            return a;
        }
    }

    @Override
    public void storeNewsIntoDatabase(String link, String title, String content) {
        News news = new News(link, content, title);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.CrawlerMapper.storeNewsIntoDatabase",news);
        }
    }

    @Override
    public void deleteFromDatabase(String link) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.delete("com.github.hcsp.CrawlerMapper.deletefromAvailableLink", link);
        }
    }

    @Override
    public void addLinkIntoDatase(String link, String tablename) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("link", link);
        map.put("tablename", tablename);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.CrawlerMapper.addLinkIntoDatase", map);
        }
    }

    @Override
    public Boolean isProcessedLink(String link) {
        Integer number = null;
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            number = session.selectOne("com.github.hcsp.CrawlerMapper.isProcessedLink", link);
        }
        if (number > 0) {
            return true;
        } else {
            return false;
        }

    }
}
