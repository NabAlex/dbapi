package app.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

    public static class PostId {
        int id;
        public PostId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static List<PostId> parseMapId(List<Map<String, Object>> objs) {
        List<PostId> p = new ArrayList<>();
        for(Map<String, Object>row: objs){
            p.add(new PostId(
                Integer.parseInt(row.get("id").toString())
            ));
        }

        return p;
    }

    public static List<Post> parseMap(List<Map<String, Object>> objs) {
        List<Post> p = new ArrayList<>();
        for(Map<String, Object>row: objs){
            p.add(new Post(
                Integer.parseInt(row.get("id").toString()),
                Integer.parseInt(row.get("parent").toString()),
                row.get("author").toString(),
                row.get("message").toString(),
                Boolean.parseBoolean(row.get("isEdited").toString()),
                row.get("forum").toString(),
                Integer.parseInt(row.get("thread_id").toString()),
                // TODO work time
                Timestamp.valueOf(row.get("created").toString())
                    .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            );
        }

        return p;
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
