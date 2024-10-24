package searchengine.crawler;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.IndexingStatus;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;

public class Crawler extends RecursiveAction {
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static boolean isIndexingStopped = false;
    private final ArrayList<Crawler> tasks = new ArrayList<>();
    protected long pageId;
    private int currentDepth = 0;
    protected String urlToParse;
    protected RootCrawler root;

    public Crawler() {
    }

    private Crawler(RootCrawler root, long pageId, String urlToParse, int currentDepth) {
        this.root = root;
        this.pageId = pageId;
        this.urlToParse = urlToParse;
        this.currentDepth = currentDepth;
    }

    @Override
    protected void compute() {
        if (isIndexingStopped) { return; }

        try {
            synchronized (root.lock) {
                ++root.indexedPageCounter;
                if ((LocalDateTime.now())
                        .isAfter(root.statusTime.plusSeconds(root.service.config.getSecondsUpdateStatistics()))) {
                    root.statusTime = LocalDateTime.now();
                    root.service.SaveSiteStatusToDatabase(root.siteId, IndexingStatus.INDEXING, "");
                    logger.info( "{} pages on site {} indexed in {} ms",
                            root.indexedPageCounter, root.baseURL, (System.currentTimeMillis() - root.startTime));
                }
            }
            DownloadPage(urlToParse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void DownloadPage(String urlToParse) throws InterruptedException, IOException {

        Thread.sleep(root.msTimeout);

        if (isIndexingStopped) { return; }

        Connection connection = Jsoup.connect(urlToParse);
        Document document;
        try {
            document = connection.get();
        } catch (HttpStatusException e) {
            logger.warn(e.getMessage());
            root.service.UpdatePageInDatabase(root.siteId, pageId, e.getStatusCode(), "Error",
                    e.getMessage() + " " + pageId + " " + urlToParse);
            return;
        }
        int statusCode = connection.response().statusCode();
        if (statusCode == 200) {
            if( currentDepth < root.maxDepth ){
                ParsePage(document);
            }
            root.service.UpdatePageInDatabase( root.siteId, pageId, statusCode, document.title(),
                    document.body().text().replaceAll("<[^>]*>","") );
        }
    }

    private void ParsePage(Document document) {

        Elements references = document.select("a");
        for (Element reference : references) {
            if (isIndexingStopped) { return; }
            ProcessReference(reference.attr("abs:href"));
        }
        tasks.forEach(Crawler::join);
    }

    private void ProcessReference(String href) {
        if (href.startsWith(root.baseURL) && href.endsWith("/") ) {
            long hrefPageId;
            synchronized (root.lock) {
                if (root.service.isPageNotParsedAlready(root.siteId, href)) {
                    try {
                        hrefPageId = root.service.SavePageToDatabase(root.siteId, href, -1, "In process");
                    }catch (MalformedURLException e){
                        logger.warn("Error parsing URL {}: {}", href, e.getMessage() );
                        return;
                    }
                } else {
                    return;
                }
            }
            Crawler crawler = new Crawler(root, hrefPageId, href, currentDepth + 1);
            crawler.fork();
            tasks.add(crawler);
        }
    }
}
