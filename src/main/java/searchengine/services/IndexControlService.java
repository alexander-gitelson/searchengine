package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.MD5;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.crawler.Crawler;
import searchengine.crawler.RootCrawler;
import searchengine.model.IndexingStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.responses.ErrorResponse;
import searchengine.responses.SuccessResponse;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexControlService {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ForkJoinPool forkJoinPool;
    public final SitesList config;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexParserService indexParserService;

    public ResponseEntity<?> startIndexing() {

        Crawler.isIndexingStopped = false;

        Object[] arrayIndexing = siteRepository.getMaxIndexingStatusTime().get(0);
        boolean nowIndexing = arrayIndexing != null &&
                ((LocalDateTime) arrayIndexing[0]).isAfter(LocalDateTime.now()
                        .minusSeconds(config.getSecondsUpdateStatistics()));

        if (nowIndexing) {
            logger.info("Indexing already started, max status time: {}", arrayIndexing[0]);
            return new ResponseEntity<>(
                    new ErrorResponse(false, "Индексация уже запущена"), HttpStatus.LOCKED);
        } else {
            if( forkJoinPool == null ){
                forkJoinPool = new ForkJoinPool(config.getParallelism());
            }
            List<SiteConfig> sitesList = config.getSites();
            for (SiteConfig siteConfig : sitesList) {
                SiteEntity siteEntity = updateSiteInfoInDatabaseBeforeIndexing(siteConfig);

                forkJoinPool.submit(new RootCrawler(siteConfig.getUrl().toLowerCase(), siteEntity.getId(), this,
                        siteConfig.getMsTimeout(), siteConfig.getMaxDepth()));
            }
            return new ResponseEntity<>(new SuccessResponse(), HttpStatus.OK);
        }
    }

    public ResponseEntity<?> stopIndexing() {
        Crawler.isIndexingStopped = true;
        return new ResponseEntity<>(new ErrorResponse( true,
                "Индексация остановлена пользователем" ), HttpStatus.OK);
    }

    private SiteEntity updateSiteInfoInDatabaseBeforeIndexing(SiteConfig siteConfig) {
        String url = siteConfig.getUrl().toLowerCase();
        List<Object[]> listId = siteRepository.getIdByURL(url);
        for (Object[] id : listId) {
            logger.info("Deleting from DB site with id={} + ...", id[0]);
            siteRepository.deleteBySiteId((long) id[0]);
            logger.info("Site with id={} deleted from DB", id[0]);
        }
        logger.info("Inserting into DB site {}...", url );
        SiteEntity siteEntity = InsertSiteToDatabase(url, siteConfig.getName(), IndexingStatus.INDEXING);
        logger.info("Site {} with id={} inserted into DB", url, siteEntity.getId());
        return siteEntity;
    }

    private SiteEntity InsertSiteToDatabase(String url, String name, IndexingStatus status) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(url);
        siteEntity.setName(name);
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteRepository.save(siteEntity);
    }

    public void SaveSiteStatusToDatabase(long id, IndexingStatus status, String lastError) {
        siteRepository.SaveSiteStatusBySiteId(id, status, lastError);
    }

    public long SavePageToDatabase(long siteId, String url, int code, String content) throws MalformedURLException {

        String path = new URL(url).getPath();

        PageEntity pageEntity = new PageEntity();

        pageEntity.setSiteId(siteId);
        pageEntity.setPath(path);
        pageEntity.setCode(code);
        pageEntity.setContent(content);
        return pageRepository.saveAndFlush(pageEntity).getId();
    }

    public void UpdatePageInDatabase(long siteId, long pageId, int code, String title, String content){
        pageRepository.updatePage(pageId, code, title, content);
        indexParserService.ProcessPageContent(siteId, pageId, content);
    }

    public boolean isPageNotParsedAlready(long siteId, String url) {
        try {
            String path = new URL(url).getPath();
            return (long) ((pageRepository.getPageCountByPath(siteId, path, MD5.hash(path)).get(0))[0]) <= 0;
        } catch (MalformedURLException e) {
            logger.warn("URL parsing error \"{}\" for URL: {}", e.getMessage(), url);
            return false;
        }
    }
}
