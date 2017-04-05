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

    public int getPosts() {
        return posts;
    }

    public int getThreads() {
        return threads;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
}