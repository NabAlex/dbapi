package app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Status {
    @JsonProperty
    private int user;
    @JsonProperty
    private int forum;
    @JsonProperty
    private int thread;
    @JsonProperty
    private int post;


    @JsonCreator
    public Status(@JsonProperty("user") int user, @JsonProperty("forum") int forum,
                  @JsonProperty("thread") int thread, @JsonProperty("post") int post){
        this.user = user;
        this.forum = forum;
        this.thread = thread;
        this.post = post;
    }

    public int getUser() {
        return user;
    }

    public int getForum() {
        return forum;
    }

    public int getThread() {
        return thread;
    }

    public int getPost() {
        return post;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public void setForum(int forum) {
        this.forum = forum;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public void setPost(int post) {
        this.post = post;
    }
}
