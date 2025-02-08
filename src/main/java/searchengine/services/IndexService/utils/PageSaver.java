package searchengine.services.IndexService.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.UserAgent;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class PageSaver extends RecursiveAction {
    private final ForkJoinPool pool;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final String url;
    private final Site site;
    private final Set<String> allLinks;
    private final UserAgent userAgent;
    private static boolean stopIndexing;

    public PageSaver(ForkJoinPool pool, SiteRepository siteRepository, PageRepository pageRepository,
                     LemmaRepository lemmaRepository, IndexRepository indexRepository,
                     Site site, String url, Set<String> allLinks, UserAgent userAgent) {
        this.pool = pool;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.site = site;
        this.url = url;
        this.allLinks = allLinks;
        this.userAgent = userAgent;
        stopIndexing = false;
    }

    public static void stopIndexing() {
        stopIndexing = true;
    }

    @Override
    protected void compute() {
        if (stopIndexing) return;

        allLinks.add(url);
        LinkParser parser = new LinkParser(site, url, userAgent);
        try {
            parser.parse();
        } catch (IOException e) {
            exceptionOfParse(e);
        }

        Page page = savePage(parser);

        if (!stopIndexing) {
            site.setStatusTime(new Date());
            siteRepository.save(site);
        }

        LemmaIndexSaver lemmaIndexSaver = new LemmaIndexSaver(lemmaRepository, indexRepository, site, page);
        try {
            lemmaIndexSaver.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<PageSaver> taskList = new ArrayList<>();
        Set<String> allLinksOfPage = parser.getAllLinksOfPage();
        allLinksOfPage.forEach(link -> {
            if (!allLinks.contains(link) && !stopIndexing) {
                PageSaver task = new PageSaver(pool, siteRepository, pageRepository, lemmaRepository,
                        indexRepository, site, link, allLinks, userAgent);
                task.fork();
                taskList.add(task);
            }
        });

        if (pool.getActiveThreadCount() == 1) {
            endIndexing();
        }
    }

    private Page savePage(LinkParser parser){
        Page page = new Page();
        page.setSite(site);
        page.setPath(parser.getPath());
        page.setCode(parser.getCode());
        page.setContent(parser.getContent());
        pageRepository.save(page);
        log.info(url);
        return page;
    }

    private void endIndexing(){
        String siteStatus = siteRepository.findStatusBySiteId(site.getId());
        if (!siteStatus.equals("FAILED")){
            log.info("** SITE {} IS INDEXED ** {}", site.getUrl(), LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            site.setStatus(Status.INDEXED);
            site.setStatusTime(new Date());
            siteRepository.save(site);
        }
    }

    private void exceptionOfParse(IOException e){
        if (url.equals(site.getUrl())) {
            site.setStatus(Status.FAILED);
            site.setStatusTime(new Date());
            site.setLastError(e + ": " + e.getMessage());
            siteRepository.save(site);
        }
        log.info("Данная ссылка привела к ошибке: {}", url);
        e.printStackTrace();
    }
}
