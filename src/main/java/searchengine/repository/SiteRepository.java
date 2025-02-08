package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;


@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query("SELECT status FROM Site WHERE id =:id")
    String findStatusBySiteId(Integer id);

    Site findByUrl(String url);
}
