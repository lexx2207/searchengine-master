package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    Lemma findByLemmaAndSite(String lemma, Site site);

    @Query("SELECT COUNT(*) FROM Lemma WHERE site_id =:siteId")
    Integer countBySite(Integer siteId);

    @Query("SELECT l FROM Lemma AS l WHERE l.frequency < 300 " +
            "AND l.lemma IN (:lemmas) AND l.site =:site " +
            "ORDER BY l.frequency ASC")
    List<Lemma> findByLemmasSetAndSiteASC(Set<String> lemmas, Site site);
}
