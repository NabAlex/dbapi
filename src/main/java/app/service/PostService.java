package app.service;

import app.models.Post;
import app.models.PostPage;
import app.models.PostUpdate;
import app.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class PostService {

    final private JdbcTemplate template;

    @Autowired
    public PostService(JdbcTemplate template) {
        this.template = template;
    }

    public void createTable() {
        String query = new StringBuilder()
                .append("CREATE TABLE IF NOT EXISTS posts ( ")
                .append("id SERIAL PRIMARY KEY, ")
                .append("parent BIGINT NOT NULL DEFAULT 0, ")
                .append("author CITEXT NOT NULL, ")
                .append("message TEXT NOT NULL, ")
                .append("isEdited BOOLEAN NOT NULL DEFAULT false, ")
                .append("forum CITEXT NOT NULL, ")
                .append("thread_id BIGINT NOT NULL, ")
                .append("created TIMESTAMP NOT NULL DEFAULT current_timestamp, ")
                .append("post_path integer[], ")
                .append("FOREIGN KEY (author) REFERENCES users(nickname), ")
                .append("FOREIGN KEY (forum) REFERENCES forums(slug), ")
                .append("FOREIGN KEY (thread_id) REFERENCES threads(id)); ")
                .toString();

        template.execute(query);
    }

    public void dropTable() {
        String query = new StringBuilder()
                .append("DROP TABLE IF EXISTS posts;").toString();

        template.execute(query);
    }

    public int getCount() {
        String query = new StringBuilder()
                .append("SELECT COUNT(*) FROM posts ;").toString();

        return template.queryForObject(query, Integer.class);
    }

    public List<Post> createManyPosts(List<Post> posts) {

        /* % - custom replace */
        String query = new StringBuilder()
                .append("INSERT INTO posts(parent, author, message, thread_id, forum, created, post_path) ")
                .append("VALUES(?,?,?,?,?,?,%) RETURNING *;").toString();

        String subQuery = new StringBuilder()
                .append("UPDATE forums SET posts = posts + 1 ")
                .append("WHERE slug = ? ;")
                .toString();

        String ifIssetParent = "WITH parent_path AS (SELECT post_path AS path FROM posts WHERE id=?) ";

        List<Post> newPosts = new ArrayList<>();

        try {
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.parse(LocalDateTime.now().toString(), DateTimeFormatter.ISO_DATE_TIME));

            for (Post post : posts) {
                String sql = query;
                String finalPath;
                if(post.getParent() == 0) {
                    finalPath = "ARRAY[(currval(pg_get_serial_sequence('posts','id')))]";
                } else {
                    sql = ifIssetParent.replace("?", post.getParent() + "") + query;
                    finalPath = "(SELECT * FROM parent_path)::BIGINT[] || (currval(pg_get_serial_sequence('posts','id')))";
                }
                sql = sql.replace("%", finalPath);
                Log.d(sql);
                Post newPost = template.queryForObject(sql, postMapper,
                        post.getParent(), post.getAuthor(), post.getMessage(), post.getThread(), post.getForum(), timestamp);

                template.update(subQuery, post.getForum());
                newPosts.add(newPost);
            }
        } catch (DataAccessException e) {
            return null;
        }
        return newPosts;
    }

    public Post update(PostUpdate postUpdate, int id) {
        StringBuilder queryBuilder = new StringBuilder()
                .append("UPDATE posts SET isEdited = true , ");

        boolean f = false;
        if (postUpdate.getMessage() != null) {
            queryBuilder.append("message = '" + postUpdate.getMessage() + "' ");
            f = true;
        }
        queryBuilder.append(" WHERE id = '" + id + "' ;");

        if (f) {
            try {
                Post oldPost = getPostById(id);
                if (oldPost.getMessage().equals(postUpdate.getMessage())) {
                    return oldPost;
                }
                template.update(queryBuilder.toString());
            } catch (DataAccessException | NullPointerException e) {
                return null;
            }
        }
        return getPostById(id);
    }

    public Post getPostById(int id) {
        String query = String.format("SELECT * FROM posts WHERE id = '%d';", id);
        Post posts = null;
        try {
            posts = template.queryForObject(query, postMapper);
        } catch (EmptyResultDataAccessException e) {}

        return posts;
    }

    public PostPage flatSort(int id, Integer limit, Integer offset, Boolean desc) {
        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT * FROM posts WHERE thread_id = ? ");

        if (desc) {
            queryBuilder.append("ORDER BY id DESC ");
        } else
            queryBuilder.append("ORDER BY id ");

        queryBuilder.append("LIMIT ? ");
        queryBuilder.append("OFFSET ? ;");
        String query = queryBuilder.toString();

        ArrayList<Post> posts = null;
        try {
            List<Map<String, Object>> rows;
            rows = template.queryForList(query, id, limit, offset);

            posts = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                posts.add(new Post(
                                Integer.parseInt(row.get("id").toString()), Integer.parseInt(row.get("parent").toString()),
                                row.get("author").toString(), row.get("message").toString(),
                                Boolean.parseBoolean(row.get("isEdited").toString()), row.get("forum").toString(),
                                Integer.parseInt(row.get("thread_id").toString()), Timestamp.valueOf(row.get("created").toString())
                                .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )
                );
            }
        } catch (DataAccessException e) {
            //System.out.println(e.getMessage());
            return null;
        }
        Integer newMarker = offset;
        if (!posts.isEmpty()) {
            newMarker += posts.size();
        }
        return new PostPage(String.valueOf(newMarker), posts);
    }

    private PostPage treeSort(int id, Integer limit, Integer offset, Boolean desc) {
        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p ")
                .append("JOIN threads AS t ON (t.id = p.thread_id) ")
                .append("WHERE t.id = ? ")
                .append("ORDER BY p.post_path ");

        if (desc)
            queryBuilder.append("DESC ");


        queryBuilder.append("LIMIT ? ");
        queryBuilder.append("OFFSET ?;");
        String query = queryBuilder.toString();

        ArrayList<Post> posts = null;
        try {
            List<Map<String, Object>> rows;
            Log.d(query);
            rows = template.queryForList(query, id, limit, offset);

            posts = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                posts.add(new Post(
                                Integer.parseInt(row.get("id").toString()), Integer.parseInt(row.get("parent").toString()),
                                row.get("author").toString(), row.get("message").toString(),
                                Boolean.parseBoolean(row.get("isEdited").toString()), row.get("forum").toString(),
                                Integer.parseInt(row.get("thread_id").toString()), Timestamp.valueOf(row.get("created").toString())
                                .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )
                );
            }
        } catch (DataAccessException e) {
            Log.d("Was error!");
            return null;
        }
        Integer newMarker = offset;
        if (!posts.isEmpty()) {
            newMarker += posts.size();
        }
        return new PostPage(String.valueOf(newMarker), posts);
    }

    public PostPage parentTreeSort(int id, Integer limit, Integer offset, Boolean desc) {
        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p ")
                .append("JOIN threads AS t ON (t.id = p.thread_id) ")
                .append("WHERE t.id = ? ")
                .append("ORDER BY p.post_path ");

        if (desc)
            queryBuilder.append("DESC ");


        queryBuilder.append("OFFSET ?;");

        String query = queryBuilder.toString();

        ArrayList<Post> posts = null;
        List<Map<String, Object>> parentRows;
        try {
            List<Map<String, Object>> rows;
            Log.d(query);
            rows = template.queryForList(query, id, offset);

            posts = new ArrayList<>();
            int count = 0;
            for (Map<String, Object> row : rows) {
                int parent_now = Integer.parseInt(row.get("parent").toString());
                if(parent_now == 0) {
                    if(desc) count++;

                    if(count >= limit)
                    {
                        if(desc)
                            posts.add(new Post(
                                            Integer.parseInt(row.get("id").toString()), parent_now,
                                            row.get("author").toString(), row.get("message").toString(),
                                            Boolean.parseBoolean(row.get("isEdited").toString()), row.get("forum").toString(),
                                            Integer.parseInt(row.get("thread_id").toString()), Timestamp.valueOf(row.get("created").toString())
                                            .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                    )
                            );
                        break;
                    }

                    if(!desc) count++;
                }

                posts.add(new Post(
                                Integer.parseInt(row.get("id").toString()), parent_now,
                                row.get("author").toString(), row.get("message").toString(),
                                Boolean.parseBoolean(row.get("isEdited").toString()), row.get("forum").toString(),
                                Integer.parseInt(row.get("thread_id").toString()), Timestamp.valueOf(row.get("created").toString())
                                .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )
                );
            }
        } catch (DataAccessException e) {
            Log.d("Was error!");
            return null;
        }
        Integer newMarker = offset;
        if (!posts.isEmpty()) {
            newMarker += posts.size();
        }
        return new PostPage(String.valueOf(newMarker), posts);
    }

    public PostPage getByThread(int id, Integer limit, String marker, String sort, Boolean desc) {
        if (marker == null) {
            marker = "0";
        }
        if (sort == null) {
            sort = "flat";
        }
        if (desc == null) {
            desc = false;
        }
        PostPage page = null;

        if (sort.toLowerCase().equals("flat")) {
            page = flatSort(id, limit, Integer.parseInt(marker), desc);
        }
        if (sort.toLowerCase().equals("tree")) {
            page = treeSort(id, limit, Integer.parseInt(marker), desc);
        }
        if (sort.toLowerCase().equals("parent_tree")) {
            page = parentTreeSort(id, limit, Integer.parseInt(marker), desc);
        }
        return page;
    }

    private final RowMapper<Post> postMapper = (rs, num) -> {
        final int id = rs.getInt("id");
        final int parent = rs.getInt("parent");
        final String author = rs.getString("author");
        final String message = rs.getString("message");
        final boolean isEdited = rs.getBoolean("isEdited");
        final String forum = rs.getString("forum");
        final int thread = rs.getInt("thread_id");
        final String created = rs.getTimestamp("created").toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new Post(id, parent, author, message, isEdited, forum, thread, created);
    };
}
