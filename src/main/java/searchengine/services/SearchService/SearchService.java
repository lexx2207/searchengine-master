package searchengine.services.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.services.IndexService.IndexService;
import searchengine.services.IndexService.utils.LemmaFinder;
import searchengine.services.SearchService.utils.RelevancePage;
import searchengine.services.SearchService.utils.SnippetFinder;
import searchengine.dto.Error;
import searchengine.dto.Response;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static String error = "";
    private static List<DataSearchItem> data;

    public Response search(String query, String url, Integer offset, Integer limit) {

        if (url != null) {
            url = IndexService.reformUrl(url);
        }

        log.info("** START SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
        log.info(" - QUERY: {}", query);

        if (query.isEmpty()){
            return new Error("Задан пустой поисковый запрос");
        }

        offset = (offset == null) ? 0 : offset;
        limit = (limit == null) ? 20 : limit;


        LemmaFinder finder = new LemmaFinder();
        Set<String> queryLemmas = finder.collectLemmas(query).keySet();
        List<Index> indexes = findIndexes(queryLemmas, url);

        if (!error.isEmpty()){
            log.info(" - ERROR: {}", error);
            log.info("** END SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            String e = error;
            error = "";
            return new Error(e);
        }

        data = getDataList(indexes);
        log.info(" RESULT SEARCH: found {} pages", data.size());
        log.info("** END SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        return buildResponse(offset, limit);
    }

    private List<Index> findIndexes(Set<String> queryLemmas, String url){
        List<Index> indexList;
        if (url == null) {
            log.info(" - SITE: ALL SITES" );
            indexList = searchByAll(queryLemmas);
        }
        else {
            log.info(" - SITE: {}", url);
            Site site = siteRepository.findByUrl(url);
            if (!site.getStatus().equals(Status.INDEXED)){
                error = "Выбранный сайт ещё не проиндексирован";
                return new ArrayList<>();
            }
            indexList = searchBySite(queryLemmas, site);
        }
        if (indexList.isEmpty() && error.isEmpty()){
            error = "Ничего не найдено";
        }
        return indexList;
    }

    private List<Index> searchByAll(Set<String> queryLemmas) {
        List<Index> indexList = new ArrayList<>();
        List<Site> allSites = siteRepository.findAll();
        for (Site site : allSites){
            if (!site.getStatus().equals(Status.INDEXED)) {
                error = "Требуется индексация / окончание индексации всех сайтов";
                return new ArrayList<>();
            }
        }

        for (Site site : allSites){
            indexList.addAll(searchBySite(queryLemmas, site));
        }

        return indexList;
    }

    private List<Index> searchBySite(Set<String> queryLemmas, Site site) {
        List<Lemma> lemmas = lemmaRepository.findByLemmasSetAndSiteASC(queryLemmas, site);
        if (queryLemmas.size() != lemmas.size()){
            return new ArrayList<>();
        }

        if (lemmas.size() == 1) {
            return indexRepository.findByLemma(lemmas.get(0));
        }

        if (lemmas.size() > 1) {
            List<Page> allPages = indexRepository.findPagesByLemma(lemmas.get(0));
            for (int i = 1; i < lemmas.size(); i++){
                if (allPages.isEmpty()){
                    return new ArrayList<>();
                }
                List<Page> pagesOfLemma = indexRepository.findPagesByLemma(lemmas.get(i));
                allPages.removeIf(page -> !pagesOfLemma.contains(page));
            }
            return indexRepository.findByLemmasAndPages(lemmas, allPages);
        }
        return new ArrayList<>();
    }

    private List<DataSearchItem> getDataList(List<Index> indexes) {
        List<RelevancePage> relevancePages = getRelevanceList(indexes);
        List<DataSearchItem> dataList = new ArrayList<>();

        for (RelevancePage page : relevancePages) {
            DataSearchItem item = new DataSearchItem();
            item.setSite(IndexService.reformUrl(page.getPage().getSite().getUrl())
                    .substring(0, IndexService.reformUrl(page.getPage().getSite().getUrl()).length() - 1));
            item.setSiteName(page.getPage().getSite().getName());
            item.setUri(page.getPage().getPath());

            String title = Jsoup.parse(page.getPage().getContent()).title();
            item.setTitle(title);
            item.setRelevance(page.getRelevance());

            String body = Jsoup.parse(page.getPage().getContent()).body().text();
            String text = title + " " + body;
            item.setSnippet( SnippetFinder.find(text, page.getRankWords().keySet()) );

            dataList.add(item);
        }

        return dataList;
    }

    private List<RelevancePage> getRelevanceList(List<Index> indexes) {
        List<RelevancePage> pageSet = new ArrayList<>();

        for (Index index : indexes) {
            RelevancePage existingPage = pageSet.stream()
                .filter(temp -> temp.getPage().equals(index.getPage())).findFirst().orElse(null);
            if (existingPage != null) {
                existingPage.putRankWord(index.getLemma().getLemma(), index.getRank());
                continue;
            }

            RelevancePage page = new RelevancePage(index.getPage());
            page.putRankWord(index.getLemma().getLemma(), index.getRank());
            pageSet.add(page);

        }

        float maxRelevance = 0.0f;

        for (RelevancePage page : pageSet) {
            float absRelevance = page.getAbsRelevance();
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        for (RelevancePage page : pageSet) {
            page.setRelevance(page.getAbsRelevance() / maxRelevance);
        }

        pageSet.sort(Comparator.comparingDouble(RelevancePage::getRelevance).reversed());
        return pageSet;
    }

    private SearchResponse buildResponse(Integer offset, Integer limit) {
        if (offset + limit >= data.size()) {
            limit = data.size() - offset;
        }
        return new SearchResponse(data.size(), data.subList(offset, offset + limit));
    }
}
