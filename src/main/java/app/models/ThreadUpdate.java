package app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ThreadUpdate {
    @JsonProperty
    private String title;
    @JsonProperty
    private String message;

    @JsonCreator
    public ThreadUpdate(@JsonProperty("title") String title,
                        @JsonProperty("message") String message){
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
