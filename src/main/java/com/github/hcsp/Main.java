package com.github.hcsp;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.*;
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        CrawlerDao dao=new MybatisCrawlerDao();
        ExecutorService executorService=Executors.newFixedThreadPool(24);
        for(int i=0;i<24;++i){
            executorService.execute(new Crawler(dao));
        }
    }
}
