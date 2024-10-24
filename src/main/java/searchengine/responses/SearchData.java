package searchengine.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchData {
	String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    double relevance;
}
