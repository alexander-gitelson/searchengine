package searchengine.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.IndexingStatus;
import searchengine.services.IndexControlService;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

public class RootCrawler extends Crawler {
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public final Long siteId;
    public long startTime;
    public volatile LocalDateTime statusTime;
    public volatile int indexedPageCounter = 0;
    public IndexControlService service;
    public int maxDepth;
    public int msTimeout;
    public String baseURL;
    public final Object lock = new Object();

    public RootCrawler(String baseURL, long siteId, IndexControlService service,
                       int msTimeout, int maxDepth) {
        this.baseURL = baseURL;
        this.siteId = siteId;
        this.service = service;
        this.msTimeout = msTimeout;
        this.maxDepth = maxDepth;
        urlToParse = baseURL;
        root = this;
    }

    @Override
    protected void compute() {
        try {
            logger.info("Indexing site {} started", baseURL);
            startTime = System.currentTimeMillis();
            statusTime = LocalDateTime.now();

            pageId = service.SavePageToDatabase(siteId, baseURL, -1, "In process");
            super.compute();

            service.SaveSiteStatusToDatabase( siteId, isIndexingStopped ?
                            IndexingStatus.FAILED : IndexingStatus.INDEXED,
                    isIndexingStopped ? "Индексация прервана" : "");

            logger.info("Indexing site {} {} in {} ms. {} pages indexed.",
                    baseURL, (isIndexingStopped ? "stopped" : "completed"),
                    (System.currentTimeMillis() - startTime), indexedPageCounter );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}