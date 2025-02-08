package searchengine.services.StatisticsService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService.IndexService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for (SiteConfig siteConfig : sitesList) {
            Site site = siteRepository.findByUrl(IndexService.reformUrl(siteConfig.getUrl()));
            if (site != null && site.getStatus().equals(Status.INDEXING)) {
                total.setIndexing(true);
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConfig.getName());
            item.setUrl(IndexService.reformUrl(siteConfig.getUrl())
                    .substring(0, IndexService.reformUrl(siteConfig.getUrl()).length() - 1));
            int pagesCount = site == null ? 0 : pageRepository.countBySite(site.getId());
            int lemmas = site == null ? 0 : lemmaRepository.countBySite(site.getId());
            item.setPages(pagesCount);
            item.setLemmas(lemmas);
            String status = site == null ? "" : site.getStatus().toString();
            item.setStatus(status);
            String error = site != null ? site.getLastError() : "Требуется индексация сайта";
            error = site != null && site.getLastError() == null ? " Ошибок не обнаружено" : error;
            item.setError(error);
            long statusTime = site == null ? new Date().getTime() : site.getStatusTime().getTime();
            item.setStatusTime(statusTime);
            total.setPages(total.getPages() + pagesCount);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
