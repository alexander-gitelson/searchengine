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
import java.util.ArrayList;
import java.util.List;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        TotalStatistics total = new TotalStatistics();

        List<SiteConfig> sitesList = sites.getSites();
        total.setSites(sitesList.size());
        total.setIndexing(true);

        for(SiteConfig site : sitesList){
            OneSiteStatistics( site, detailed, total );
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private void OneSiteStatistics( SiteConfig site, List<DetailedStatisticsItem>  detailed, TotalStatistics total ){

        String url = site.getUrl().toLowerCase();
        List<Object[]> listSiteData = siteRepository.getSiteDataByURL(url);
        if (listSiteData.isEmpty()) {
            return;
        }

        long siteId = (long) listSiteData.get(0)[0];
        long pages = pageRepository.countPageEntityBySiteId( siteId );
        long lemmas = lemmaRepository.countLemmaEntityBySiteId( siteId );
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(pages);
        item.setLemmas(lemmas);

        item.setStatus( ((IndexingStatus)listSiteData.get(0)[1]).toString() );
        ZonedDateTime zonedDateTime = ZonedDateTime.of((LocalDateTime)listSiteData.get(0)[2], ZoneId.systemDefault());
        item.setStatusTime( zonedDateTime.toInstant().toEpochMilli() );
        item.setError( (String)listSiteData.get(0)[3] );
        total.setPages(total.getPages() + pages);
        total.setLemmas(total.getLemmas() + lemmas);
        detailed.add(item);
    }
}
