package app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Forum{
    @JsonProperty
    private int posts;
    @JsonProperty
    private String slug;
    @JsonProperty
    private int threads;
    @JsonProperty
    private String title;
    @JsonProperty
    private String user;

    @JsonCreator
    public Forum(@JsonProperty("title") String title, @JsonProperty("user") String user,
                 @JsonProperty("slug") String slug, @JsonProperty("posts") int posts,
                 @JsonProperty("threads") int threads){
        this.title = title;
        this.user = user;
        this.slug = slug;
        this.posts = posts;
        this.threads = threads;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getUser() {
        return user;
    }

    public void setNick(String nick) {
        this.user = nick;
    }
}