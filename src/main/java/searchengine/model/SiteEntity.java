package searchengine.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@NoArgsConstructor
@Getter
@Setter
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private IndexingStatus status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", length=1024, updatable = false)
    private String lastError;

    @Column(name = "url", length=255, nullable = false, updatable = false)
    private String url;

    @Column(name = "name", length=255, nullable = false, updatable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY )
    @JoinColumn(name = "site_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PageEntity> pages;
}