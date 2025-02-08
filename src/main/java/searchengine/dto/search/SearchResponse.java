package searchengine.dto.search;

import lombok.Data;
import searchengine.dto.Response;

import java.util.List;

@Data
public class SearchResponse extends Response {
    private boolean result;
    private Integer count;
    private List<DataSearchItem> data;

    public SearchResponse(int count, List<DataSearchItem> data) {
        result = true;
        this.count = count;
        this.data = data;
    }
}
