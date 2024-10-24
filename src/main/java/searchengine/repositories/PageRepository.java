package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {

    long countPageEntityBySiteId( long siteId );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM PageEntity WHERE pathMD5 = :pathMD5 and path = :path and siteId = :siteId")
    void deletePageByPath( long siteId, String path, String pathMD5 );

    @Query(value = "SELECT count(*) FROM PageEntity WHERE pathMD5 = :pathMD5 and path = :path and siteId = :siteId")
    List<Object[]> getPageCountByPath( long siteId, String path, String pathMD5 );

    @Transactional
    @Modifying
    @Query(value = "UPDATE PageEntity set code = :code, title = :title, content = :content " +
            "WHERE id = :pageId")
    void updatePage( long pageId, int code, String title, String content );
}

