package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface LemmaRepository  extends JpaRepository<LemmaEntity, Long> {

    List<LemmaEntity> findBySiteIdAndLemma(long siteId, String lemma );

    long countLemmaEntityBySiteId( long siteId );

    @Transactional
    @Modifying
    @Query(value = "UPDATE LemmaEntity SET frequency = frequency + 1 WHERE id = :lemmaId")
    void IncrementFrequency( long lemmaId );

    @Query(value =
            "SELECT count(*) FROM " +
                    "( SELECT i.pageId pageId " +
                    "FROM LemmaEntity l, IndexEntity i " +
                    "WHERE l.lemma IN ( :lemmasList ) AND i.lemmaId = l.id " +
                    "AND ( l.siteId = :siteId OR :siteId is null )" +
                    "GROUP BY i.pageId " +
                    "HAVING count(*) = :lemmasCount )")
    List<Object[]> countFoundedPages( Long siteId, ArrayList<String> lemmasList, int lemmasCount );


    @Query(value =
        "SELECT s.url, s.name, p.path, p.title, p.content, q.relevance " +
                "FROM SiteEntity s, PageEntity p, " +
        "( SELECT i.pageId pageId, sum( i.rank ) relevance " +
                "FROM LemmaEntity l, IndexEntity i " +
        "WHERE l.lemma IN ( :lemmasList ) AND i.lemmaId = l.id " +
                "AND ( l.siteId = :siteId OR :siteId is null )" +
        "GROUP BY i.pageId " +
        "HAVING count(*) = :lemmasCount ) q "+
        "WHERE p.id = q.pageId AND s.id = p.siteId " +
                "ORDER BY q.relevance DESC " +
                "LIMIT :limit OFFSET :offset")
    List<Object[]> getFoundedPages( Long siteId, ArrayList<String> lemmasList, int lemmasCount, int limit, int offset );
}

