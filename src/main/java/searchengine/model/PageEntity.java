package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Table(name = "page", indexes = {@Index(name="IndexPathMD5", columnList = "path_md5")})
@NoArgsConstructor
@Getter
@Setter
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "path", columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(name = "path_md5", columnDefinition = "CHAR(32) AS (MD5(path))", insertable=false)
    private String pathMD5;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "title", length=255)
    private String title;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(fetch = FetchType.LAZY )
    @JoinColumn(name = "page_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<IndexEntity> indexes;

}