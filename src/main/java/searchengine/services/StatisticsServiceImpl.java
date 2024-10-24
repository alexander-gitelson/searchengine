package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.IndexingStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    List<DetailedStatisticsItem> detailed = new ArrayList<>();
    List<Object[]> listSiteData;
    TotalStatistics total = new TotalStatistics();

    @Override
    public StatisticsResponse getStatistics() {

        List<SiteConfig> sitesList = sites.getSites();
        total.setSites(sitesList.size());
        total.setIndexing(true);

        for(SiteConfig site : sitesList){
            String url = site.getUrl().toLowerCase();
            listSiteData = siteRepository.getSiteDataByURL(url);
            if (listSiteData.isEmpty()) {
                continue;
            }
            OneSiteStatistics( site );
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private void OneSiteStatistics( SiteConfig site ){
        long siteId = (long) listSiteData.get(0)[0];
        long pages = pageRepository.countPageEntityBySiteId( siteId );
        long lemmas = lemmaRepository.countLemmaEntityBySiteId( siteId );
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(pages);
        item.setLemmas(lemmas);

        item.setStatus( ((IndexingStatus)listSiteData.get(0)[1]).toString() );
        item.setStatusTime( ((LocalDateTime)listSiteData.get(0)[2])
                .format(DateTimeFormatter.ofPattern( "dd.MM.yyyy HH:mm:ss" )));
        item.setError( (String)listSiteData.get(0)[3] );
        total.setPages(total.getPages() + pages);
        total.setLemmas(total.getLemmas() + lemmas);
        detailed.add(item);
    }
}
