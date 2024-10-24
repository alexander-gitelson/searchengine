package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexControlService;
import searchengine.services.IndexOnePageService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexControlService indexControlService;
    private final SearchService searchService;
    private final IndexOnePageService indexOnePageService;

    public ApiController(StatisticsService statisticsService, IndexControlService indexControlService,
                         SearchService searchService, IndexOnePageService indexOnePageService) {
        this.statisticsService = statisticsService;
        this.indexControlService = indexControlService;
        this.searchService = searchService;
        this.indexOnePageService = indexOnePageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping(value = {"/startIndexing/", "/startIndexing"})
    public ResponseEntity<?> startIndexing() {
        return indexControlService.startIndexing();
    }

    @GetMapping(value = {"/stopIndexing/", "/stopIndexing"})
    public ResponseEntity<?> stopIndexing() {
        return indexControlService.stopIndexing();
    }

    @PostMapping(value = {"/indexPage/", "/indexPage"})
    public ResponseEntity<?> create(@RequestBody String url) {
        return indexOnePageService.indexPage(url);
    }

    @GetMapping(value = {"/search/", "/search"})
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam int offset, @RequestParam int limit,
            @RequestParam(required = false) String site ) {
        return searchService.search( query, offset, limit, site );
    }
}
