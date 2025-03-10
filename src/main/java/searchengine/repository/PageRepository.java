package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;


@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Page findByPathAndSite(String path, Site site);

    @Query("SELECT COUNT(*) FROM Page WHERE site_id =:siteId")
    Integer countBySite(Integer siteId);
}
