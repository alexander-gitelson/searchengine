package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Entity
@Table(name = "lemma", indexes = {@Index(name="LemmaSiteId", columnList = "lemma, site_id", unique = true)})
@NoArgsConstructor
@Getter
@Setter
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "lemma", length=255, nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @OneToMany(fetch = FetchType.LAZY )
    @JoinColumn(name = "lemma_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<IndexEntity> indexes;
}
