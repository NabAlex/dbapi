package app.models;

import app.util.TimeWork;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.Map;


public class Thread {
    @JsonProperty
    private String title;
    @JsonProperty
    private String author;
    @JsonProperty
    private String forum;
    @JsonProperty
    private String message;
    @JsonProperty
    private int votes;
    @JsonProperty
    private String slug;
    @JsonProperty
    private String created;
    @JsonProperty
    private int id;

    @JsonCreator
    public Thread(@JsonProperty("id") int id,
                  @JsonProperty("title") String title,
                  @JsonProperty("author") String author,
                  @JsonProperty("forum") String forum,
                  @JsonProperty("message") String message,
                  @JsonProperty("votes") int votes,
                  @JsonProperty("slug") String slug,
                  @JsonProperty("created") String created ){
        this.id = id;
        this.title = title;
        this.author = author;
        this.forum = forum;
        this.message = message;
        this.votes = votes;
        this.slug = slug;
        this.created = created;
    }
    
    public static Thread getThreadByRow(Map<String, Object> row) {
        Object slugObj = row.get("slug");
        String slugStr = (slugObj != null) ? slugObj.toString() : null;
        
        return new Thread(
            Integer.parseInt(row.get("id").toString()),
            row.get("title").toString(),
            row.get("author").toString(),
            row.get("forum").toString(),
            row.get("message").toString(),
            Integer.parseInt(row.get("votes").toString()),
            slugStr,
            TimeWork.getIsoTime( Timestamp.valueOf(row.get("created").toString()) )
        );
    }
    
    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public int getVotes() {
        return votes;
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

    public String getSlug() {
        return slug;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public void setVotes(int votes) {
        this.votes = votes;
    }
}
