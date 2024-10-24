package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "`index`", indexes = {@Index(name="LemmaIdPageId", columnList = "lemma_id, page_id", unique = true)})
@NoArgsConstructor
@Getter
@Setter
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "lemma_id", nullable = false)
    private Long lemmaId;

    @Column(name = "`rank`", nullable = false)
    private float rank;
}
