package searchengine.controllers;

import org.springframework.web.bind.annotation.*;
import searchengine.dto.Response;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService.IndexService;
import searchengine.services.SearchService.SearchService;
import searchengine.services.StatisticsService.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexService indexService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public Response startIndexing() {
        return indexService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public Response stopIndexing() {
        return indexService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public Response indexPage(@RequestParam(name = "url") String url) {
        return indexService.indexPage(url);
    }

    @GetMapping("/search")
    public Response search(@RequestParam String query, @RequestParam(required = false) String site,
    @RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit) {
        return searchService.search(query, site, offset, limit);
    }
}
