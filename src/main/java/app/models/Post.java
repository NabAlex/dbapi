package app.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Array;
import java.sql.SQLException;


public class Post {
    @JsonProperty
    private int id;
    @JsonProperty
    private int parent;
    @JsonProperty
    private boolean isEdited;
    @JsonProperty
    private String author;
    @JsonProperty
    private String forum;
    @JsonProperty
    private String message;
    @JsonProperty
    private int thread;
    @JsonProperty
    private String created;

    @JsonCreator
    public Post(@JsonProperty("id") int id, @JsonProperty("parent") int parent,
                @JsonProperty("author") String author, @JsonProperty("message") String message,
                @JsonProperty("isEdited") boolean isEdited, @JsonProperty("forum") String forum,
                @JsonProperty("thread") int thread, @JsonProperty("created") String created){
        this.id = id;
        this.parent = parent;
        this.author = author;
        this.forum = forum;
        this.message = message;
        this.thread = thread;
        this.isEdited = isEdited;
        this.created = created;
    }

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public int getParent() {
        return parent;
    }

    public int getThread() {
        return thread;
    }

    public String getCreated() {
        return created;
    }

    public String getForum() {
        return forum;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
