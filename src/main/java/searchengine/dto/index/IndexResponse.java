package searchengine.dto.index;

import lombok.Data;
import searchengine.dto.Response;

@Data
public class IndexResponse extends Response {
    private boolean result;

    public IndexResponse() {
        result = true;
    }
}
