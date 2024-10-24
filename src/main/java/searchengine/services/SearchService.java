package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.IndexingStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.responses.ErrorResponse;
import searchengine.responses.SearchData;
import searchengine.responses.SearchResponse;
import searchengine.responses.SuccessResponse;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SitesList config;
//    public final long secondsUpdateStatistics = 10L;
//    private final int snippetDepth = 8;
    private final String itemDelimiter = ". . . ";
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private Long siteId = null;

    public ResponseEntity<?> search(String query, int offset, int limit, String siteURL) {

        logger.info("Got search request: query={} offset={} limit={} site={}", query, offset, limit, siteURL);

        ResponseEntity<?> checkSiteResponse = CheckSite(siteURL);
        if (checkSiteResponse.getStatusCode() != HttpStatus.OK) {
            return checkSiteResponse;
        }

        ArrayList<String> lemmasList = new ArrayList<>();
        HashSet<String> wordsToBold = new HashSet<>();
        GenerateLemmasListAndWordsToBold(query, lemmasList, wordsToBold);

        List<Object[]> foundedPages = lemmaRepository.getFoundedPages(siteId, lemmasList, lemmasList.size(),
                limit, offset);

        List<Object[]> pagesCount = lemmaRepository.countFoundedPages(siteId, lemmasList, lemmasList.size() );

        SearchResponse searchResponse = GenerateResponse(foundedPages, pagesCount, wordsToBold);

        return new ResponseEntity<>(searchResponse, HttpStatus.OK);
    }

    private ResponseEntity<?> CheckSite(String siteURL) {
        try {
            if (siteURL != null && !siteURL.isEmpty()) {
                return CheckNonEmptySite( siteURL );
            } else {
                return CheckEmptySite();
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(false,
                    String.format("Error while checking site %s: %s", siteURL, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> CheckNonEmptySite(String siteURL) {
        List<Object[]> listSiteData = siteRepository.getSiteDataByURL(siteURL);
        if (!listSiteData.isEmpty()) {
            if (listSiteData.get(0)[1] == IndexingStatus.INDEXED) {
                siteId = (Long) listSiteData.get(0)[0];
                return new ResponseEntity<>(new SuccessResponse(true), HttpStatus.OK);
            } else {
                String errorMessage = String.format("Site %s status is not INDEXED", siteURL);
                logger.error(errorMessage);
                return new ResponseEntity<>(
                        new ErrorResponse(false, errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            String errorMessage = String.format("Site %s not found", siteURL);
            logger.error(errorMessage);
            return new ResponseEntity<>(new ErrorResponse(false, errorMessage),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> CheckEmptySite() {
        Object[] arrayIndexing = siteRepository.getMaxIndexingStatusTime().get(0);
        boolean nowIndexing = arrayIndexing != null && ((LocalDateTime) arrayIndexing[0]).isAfter(LocalDateTime.now()
                .minusSeconds(config.getSecondsUpdateStatistics()));

        if (nowIndexing) {
            String errorMessage = "Indexing is started now";
            logger.info(errorMessage);
            return new ResponseEntity<>(
                    new ErrorResponse(false, errorMessage), HttpStatus.LOCKED);
        } else {
            return new ResponseEntity<>(new SuccessResponse(true), HttpStatus.OK);
        }
    }

    private void GenerateLemmasListAndWordsToBold(String query,
                                                 ArrayList<String> lemmasList, HashSet<String> wordsToBold) {
        final String[] particlesNames = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            String errorMessage = "Exception while creating RussianLuceneMorphology: " + e.getMessage();
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }

        String[] words = query.split("\\P{IsCyrillic}+");
        for (String word : words) {
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word.toLowerCase());
            for (String wbf : wordBaseForms) {
                String[] lemmaParts = wbf.split("[| ]");
                if (!Arrays.stream(particlesNames).toList().contains(lemmaParts[2])) {
                    wordsToBold.add(word);
                    wordsToBold.add(lemmaParts[0]);
                    lemmasList.add(lemmaParts[0]);
                }
            }
        }
    }

    private SearchResponse GenerateResponse(List<Object[]> foundedPages, List<Object[]> pagesCount,
                                            HashSet<String> wordsToBold) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        if( pagesCount == null || pagesCount.isEmpty() ) {
            searchResponse.setCount(0);
        }else {
            searchResponse.setCount( (long)pagesCount.get(0)[0] );
        }

        for (Object[] foundedPage : foundedPages) {

            String url = (String) foundedPage[0];
            String name = (String) foundedPage[1];
            String path = (String) foundedPage[2];
            String title = (String) foundedPage[3];
            String content = (String) foundedPage[4];
            double relevance = (double) foundedPage[5];

            searchResponse.getData().add(new SearchData(url, name, path, title,
                    GenerateSnippet(content, wordsToBold), relevance));
        }
        return searchResponse;
    }

    private String GenerateSnippet(String content, Set<String> wordsToBold) {
        String[] words = content.split("\\P{IsCyrillic}+");
        boolean[] inSnippet = new boolean[words.length];
        boolean[] isBold = new boolean[words.length];
        StringBuilder sb = new StringBuilder();

        SetInSnippetFlags(words, inSnippet, isBold, wordsToBold);

        InsertWordsToSnippet(words, inSnippet, isBold, sb);

        if (sb.isEmpty()) {
            for (int i = 0; i < Math.min(words.length, config.getSnippetDepth() * config.getSnippetDepth()); i++) {
                sb.append(words[i]);
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    private void SetInSnippetFlags(String[] words, boolean[] inSnippet, boolean[] isBold, Set<String> wordsToBold) {
        for (int i = 0; i < inSnippet.length; i++) {
            if (wordsToBold.contains(words[i])) {
                isBold[i] = true;
                for (int j = Math.max(0, i - config.getSnippetDepth());
                     j <= Math.min(words.length, i + config.getSnippetDepth()); j++) {
                    inSnippet[j] = true;
                }
            }
        }
    }

    private void InsertWordsToSnippet(String[] words, boolean[] inSnippet, boolean[] isBold, StringBuilder sb) {
        boolean prevInSnippet = false;
        for (int i = 0; i < words.length; i++) {
            if (isBold[i]) {
                sb.append("<b>");
                sb.append(words[i]);
                sb.append("</b> ");
            } else {
                if (inSnippet[i]) {
                    if( sb.isEmpty() && i > 0 ){
                        sb.append(itemDelimiter);
                    }
                    sb.append(words[i]);
                    sb.append(" ");
                } else {
                    if (prevInSnippet) {
                        sb.append(itemDelimiter);
                    }
                }
            }
            prevInSnippet = inSnippet[i];
        }
    }
}
