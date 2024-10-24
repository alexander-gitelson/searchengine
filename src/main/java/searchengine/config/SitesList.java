package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private int parallelism = Runtime.getRuntime().availableProcessors();
    private final long secondsUpdateStatistics = 60L;
    private final int snippetDepth = 4;
    private List<SiteConfig> sites;
}
