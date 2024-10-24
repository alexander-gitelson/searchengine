package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long>{
    List<IndexEntity> findByPageIdAndLemmaId(long pageId, long lemmaId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE IndexEntity set rank = rank + 1 where id = :indexId")
    void IncrementRank( long indexId );
}


