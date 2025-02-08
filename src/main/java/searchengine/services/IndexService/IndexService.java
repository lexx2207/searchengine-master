package searchengine.services.IndexService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.services.IndexService.utils.LemmaIndexSaver;
import searchengine.services.IndexService.utils.LinkParser;
import searchengine.services.IndexService.utils.PageSaver;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.Error;
import searchengine.dto.Response;
import searchengine.dto.index.IndexResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final UserAgent userAgent;

    public Response startIndexing() {
        if (isIndexing()) {
            return new Error("Индексация уже запущена");
        }

        log.info("** START INDEXING ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        for (SiteConfig siteConfig : sitesList.getSites()) {
            new Thread(() -> {
                Site site = new Site();
                site.setStatus(Status.INDEXING);
                site.setStatusTime(new Date());
                site.setUrl(IndexService.reformUrl(siteConfig.getUrl()));
                site.setName(siteConfig.getName());
                siteRepository.save(site);

                Set<String> allLinks = ConcurrentHashMap.newKeySet();
                allLinks.add(site.getUrl());
                ForkJoinPool pool = new ForkJoinPool();
                pool.invoke(new PageSaver(pool, siteRepository, pageRepository, lemmaRepository, indexRepository,
                    site, site.getUrl(), allLinks, userAgent));
            }).start();
        }
        return new IndexResponse();
    }

    public Response stopIndexing() {
        if (!isIndexing()){
            return new Error("Индексация не запущена");
        }

        PageSaver.stopIndexing();
        log.info("** STOP INDEXING ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                site.setStatus(Status.FAILED);
                site.setStatusTime(new Date());
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }

        return new IndexResponse();
    }

    public synchronized Response indexPage(String url){
        if (url.trim().isEmpty()) {
            return new Error("Страница не указана");
        }
        url = url.trim();

        SiteConfig siteConfig = findSiteCfgByUrl(url);
        if (siteConfig == null){
            return new Error("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        String siteUrl = siteConfig.getUrl();
        Site site = siteRepository.findByUrl(IndexService.reformUrl(siteUrl));
        if (site == null) {
            return new Error("Сайт данной страницы не был проиндексирован. " +
                    "Требуется индексация!");
        }

        String path = reformUrl(url).replaceAll(reformUrl(siteConfig.getUrl()), "/");

        Page oldPage = pageRepository.findByPathAndSite(path, site);
        if (oldPage != null){
            deletePage(oldPage);
        }

        LinkParser parser = new LinkParser(site, IndexService.reformUrl(url), userAgent);
        return buildResponse(parser);
    }

    private Response buildResponse(LinkParser parser){
        Site site = parser.getSite();
        String url = parser.getUrl();
        try {
            parser.parse();
            int code = parser.getCode();
            if (code >= 400 && code <= 599) {
                return new Error("Код ответа страницы: " + code);
            }else {
                Page page = savePage(parser);
                new LemmaIndexSaver(lemmaRepository, indexRepository, site, page).save();
                log.info("** PAGE {} IS INDEXED ** {}", url, LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
                site.setStatusTime(new Date());
                siteRepository.save(site);

                return new IndexResponse();
            }
        } catch (IOException e) {
            site.setStatus(Status.FAILED);
            site.setStatusTime(new Date());
            site.setLastError(e + ": " + e.getMessage());
            siteRepository.save(site);
            log.info("Данная ссылка привела к ошибке: {}", url);
            e.printStackTrace();
            return new Error("Индексация ссылки привела к ошибке. " + e + ": " + e.getMessage());
        }
    }

    private boolean isIndexing() {
        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (site.getStatus().equals(Status.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    public static String reformUrl(String url) {
        String reformUrl = url.contains("://www.") ? url.replace("://www.", "://") : url;
        reformUrl = reformUrl.endsWith("/") ? reformUrl : reformUrl.concat("/");
        return reformUrl;
    }

    private SiteConfig findSiteCfgByUrl(String url){
        for (SiteConfig siteConfig : sitesList.getSites()){
            if (reformUrl(url).contains(reformUrl(siteConfig.getUrl()))){
                return siteConfig;
            }
        }
        return null;
    }

    private void deletePage(Page page){
        List<Index> indexesOfPage = indexRepository.findByPage(page);
        indexesOfPage.forEach(index ->{
            Lemma lemma = index.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);
            lemmaRepository.save(lemma);
        });
        indexRepository.deleteAll(indexesOfPage);
        pageRepository.delete(page);
    }

    private Page savePage(LinkParser parser){
        Page page = new Page();
        page.setSite(parser.getSite());
        page.setPath(parser.getPath());
        page.setCode(parser.getCode());
        page.setContent(parser.getContent());
        pageRepository.save(page);
        return page;
    }
}
