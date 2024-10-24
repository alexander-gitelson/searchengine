package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;


@Service
@RequiredArgsConstructor
public class IndexParserService {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//    public final long secondsUpdateStatistics = 10L;
    private final SitesList config;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public void ProcessPageContent( long siteId, long pageId, String content ) {

        long statusTime = System.currentTimeMillis();
        int wordCounter = 0;

        LuceneMorphology luceneMorph;
        try{
            luceneMorph = new RussianLuceneMorphology();
        }catch ( IOException e ){
            logger.error( "Exception while creating RussianLuceneMorphology: {}", e.getMessage() );
            return;
        }

        String[] words = content.split("\\P{IsCyrillic}+");
        for (String word : words) {
            ProcessWord( siteId, pageId, word, luceneMorph );
            ++wordCounter;
            if (System.currentTimeMillis() > statusTime + config.getSecondsUpdateStatistics() * 1000) {
                statusTime = System.currentTimeMillis();
                logger.info("{} words processed on pageId={} siteId={}", wordCounter, pageId, siteId);
            }
        }
    }

    private void ProcessWord(long siteId, long pageId, String word, LuceneMorphology luceneMorph) {

        final String[] particlesNames = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

        if (word.length() > 2) {
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word.toLowerCase());
            for (String wbf : wordBaseForms) {
                String[] lemmaParts = wbf.split("[| ]");
                if (!Arrays.stream(particlesNames).toList().contains(lemmaParts[2])) {
                    ProcessLemma(siteId, pageId, lemmaParts[0]);
                }
            }
        }
    }

    private void ProcessLemma(long siteId, long pageId, String lemma) {
        List<LemmaEntity> listLemma = lemmaRepository.findBySiteIdAndLemma(siteId, lemma);
        if (listLemma.isEmpty()) {
            ProcessNewLemma(siteId, pageId, lemma);
        } else {
            ProcessExistingLemma(pageId, listLemma.get(0).getId());
        }
    }

    private void ProcessNewLemma(long siteId, long pageId, String lemma) {
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setSiteId(siteId);
        lemmaEntity.setFrequency(1);
        try {
            lemmaEntity = lemmaRepository.save(lemmaEntity);
        }catch (DataIntegrityViolationException e ) {
            List<LemmaEntity> listLemma = lemmaRepository.findBySiteIdAndLemma( siteId, lemma);
            ProcessExistingLemma(pageId, listLemma.get(0).getId());
            return;
        }
        ProcessNewIndex( pageId, lemmaEntity.getId() );
    }

    private void ProcessExistingLemma(long pageId, long lemmaId) {

        lemmaRepository.IncrementFrequency( lemmaId );

        List<IndexEntity> listIndex = indexRepository.findByPageIdAndLemmaId( pageId, lemmaId );
        if (listIndex.isEmpty()) {
            ProcessNewIndex( pageId, lemmaId );
        } else {
            ProcessExistingIndex( listIndex.get(0).getId() );
        }
    }

    private void ProcessNewIndex(long pageId, long lemmaId ) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageId);
        indexEntity.setLemmaId(lemmaId);
        indexEntity.setRank(1);
        try {
            indexRepository.save(indexEntity);
        }catch (DataIntegrityViolationException e) {
            List<IndexEntity> listIndex = indexRepository.findByPageIdAndLemmaId( pageId, lemmaId );
            ProcessExistingIndex( listIndex.get(0).getId() );
        }
    }

    private void ProcessExistingIndex(long indexId) {
        indexRepository.IncrementRank(indexId);
    }
}
