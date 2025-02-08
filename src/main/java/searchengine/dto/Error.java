package searchengine.dto;

import lombok.Data;

@Data
public class Error extends Response {
    private boolean result;
    private String error;

    public Error(String error) {
        result = false;
        this.error = error;
    }
}
