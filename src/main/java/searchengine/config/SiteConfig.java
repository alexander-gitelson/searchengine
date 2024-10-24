package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SiteConfig {
    private String url;
    private String name;
    private int msTimeout = 500;
    private int maxDepth = 16; 
}
