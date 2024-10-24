package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

    @Query(value = "SELECT max( statusTime ) FROM SiteEntity WHERE status = \"INDEXING\"")
    List<Object[]> getMaxIndexingStatusTime();

    @Query(value = "SELECT id FROM SiteEntity WHERE url = :url")
    List<Object[]> getIdByURL( String url );

    @Query(value = "SELECT id, status, statusTime, lastError FROM SiteEntity WHERE url = :url")
    List<Object[]> getSiteDataByURL( String url );

    @Query(value = "SELECT id FROM SiteEntity WHERE url like '%' || :host || '%' ORDER BY length( url )")
    List<Object[]> getIdByHost( String host );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM SiteEntity WHERE id = :siteId")
    void deleteBySiteId( long siteId );

    @Transactional
    @Modifying
    @Query(value = "UPDATE SiteEntity SET status = :status, lastError = :lastError, statusTime = current_timestamp() " +
            "WHERE id = :siteId")
    void SaveSiteStatusBySiteId(long siteId, IndexingStatus status, String lastError);
}
