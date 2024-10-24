package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.MD5;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.responses.ErrorResponse;
import searchengine.responses.SuccessResponse;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexOnePageService {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Thread threadOnePageIndexing;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexParserService indexParserService;
    private final IndexControlService indexControlService;


    public ResponseEntity<?> indexPage(String url) {
        try {
            CheckPreviousIndexing();
            url = DecodeURL( url );
            URL urlObj = GetURLObj(url);
            String host = urlObj.getHost();
            String path = urlObj.getPath();
            long siteId = GetSiteId(host);
            return ConnectAndParsePage(url, path, siteId);
        }catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(false, e.getMessage()), HttpStatus.OK);
        }
    }

    private void CheckPreviousIndexing(){
        if (threadOnePageIndexing != null && threadOnePageIndexing.isAlive()) {
            String ErrorMessage = "Не завершена предыдущая индексация";
            logger.warn( ErrorMessage );
            throw new RuntimeException(ErrorMessage);
        }
    }

    private String DecodeURL(String url) {
        try {
            url = url.substring( url.indexOf( '=' ) + 1 ).toLowerCase();
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            String ErrorMessage = String.format( "URL %s decoding error: %s", url, e.getMessage() );
            logger.warn( ErrorMessage );
            throw new RuntimeException(ErrorMessage);
        }
    }

    private URL GetURLObj(String url) {
        try {
            return new URL(url);
        } catch (Exception e) {
            String ErrorMessage = String.format( "URL %s parsing error: %s", url, e.getMessage() );
            logger.warn( ErrorMessage );
            throw new RuntimeException(ErrorMessage);
        }
    }

    private long GetSiteId(String host) {
        List<Object[]> listId = siteRepository.getIdByHost(host);
        if (!listId.isEmpty()) {
            return (long) listId.get(0)[0];
        } else {
            logger.warn("Site not found for host: {}", host);
            throw new RuntimeException("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
    }

    private ResponseEntity<?> ConnectAndParsePage(String url, String path, long siteId) {
        try {
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();
            int statusCode = connection.response().statusCode();

            if (statusCode == 200) {
                String content = document.text().replaceAll("<[^>]*>", "");
                pageRepository.deletePageByPath(siteId, path, MD5.hash(path));
                long pageId = indexControlService.SavePageToDatabase(siteId, url, statusCode, content);
                (threadOnePageIndexing = new Thread(() ->
                        indexParserService.ProcessPageContent(siteId, pageId, content))).start();
                logger.info("Parsing page {} started", url);
                return new ResponseEntity<>(new SuccessResponse(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ErrorResponse(false, "HTTP status code: " + statusCode),
                        HttpStatus.OK);
            }
        } catch (Exception e) {
            String ErrorMessage = String.format( "Connect or indexing error: %s", e.getMessage() );
            logger.warn(ErrorMessage);
            return new ResponseEntity<>(new ErrorResponse(false, ErrorMessage), HttpStatus.OK);
        }
    }
}
