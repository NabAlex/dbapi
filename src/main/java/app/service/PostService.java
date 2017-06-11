package app.service;

import app.models.Post;
import app.models.PostWithMarker;
import app.models.PostUpdate;
import app.service.util.BatchManagerService;
import app.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Transactional
public class PostService {
    @Autowired
    private JdbcTemplate template;

    @Autowired
    private UserInForumService userInForumService;

    @Autowired
    BatchManagerService batchManagerService;
    
    interface CommandStatic {
        String createTable = "CREATE TABLE IF NOT EXISTS posts (" +
            "id SERIAL PRIMARY KEY, " +
            "parent BIGINT NOT NULL DEFAULT 0, " +
            "author CITEXT NOT NULL, " +
            "message TEXT NOT NULL, " +
            "isEdited BOOLEAN NOT NULL DEFAULT false, " +
            "forum CITEXT NOT NULL, " +
            "thread_id BIGINT NOT NULL, " +
            "created TIMESTAMP NOT NULL DEFAULT current_timestamp, " +
            "post_path integer[], " +
            "FOREIGN KEY (author) REFERENCES users(nickname), " +
            "FOREIGN KEY (forum) REFERENCES forums(slug), " +
            "FOREIGN KEY (thread_id) REFERENCES threads(id)); ";

        String dropTable = "DROP TABLE IF EXISTS posts;";
        String truncateTable = "TRUNCATE TABLE posts CASCADE;";
        
        String getById = "SELECT * FROM posts WHERE id=?";

        String insertIntoPosts = "INSERT INTO posts(id, parent, author, message, thread_id, forum, created, post_path) " +
            "VALUES(?,?,?,?,?,?,?,array_append((SELECT post_path FROM posts WHERE id = ?), ?));";

        String selectFlatSort = "SELECT * FROM posts WHERE thread_id=? ORDER BY id LIMIT ? OFFSET ?;";
        String selectFlatSortDesc = "SELECT * FROM posts WHERE thread_id=? ORDER BY id DESC LIMIT ? OFFSET ?;";

        String selectTreeSort = "SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p " +
            "JOIN threads AS t ON (t.id = p.thread_id) " +
            "WHERE t.id = ? " +
            "ORDER BY p.post_path LIMIT ? OFFSET ?";
        String selectTreeSortDesc = "SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p " +
            "JOIN threads AS t ON (t.id = p.thread_id) " +
            "WHERE t.id = ? " +
            "ORDER BY p.post_path DESC LIMIT ? OFFSET ?;";

        String selectParentPostsDesc = "SELECT id, created FROM posts WHERE thread_id = ? AND parent = 0 ORDER BY id DESC LIMIT ? OFFSET ?;";
        String selectParentPosts = "SELECT id, created FROM posts WHERE thread_id = ? AND parent = 0 ORDER BY id LIMIT ? OFFSET ?;";
            ;
        String selectChildPosts = "SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p " +
            "WHERE p.post_path[1] = ? ORDER BY post_path;";

        String selectChildPostsDesc = "SELECT p.id, p.parent, p.author, p.message, p.isEdited, p.forum, p.thread_id, p.created FROM posts AS p " +
            "WHERE p.post_path[1] = ? ORDER BY post_path DESC;";

        String update = "UPDATE posts SET isEdited = true, message=? WHERE id=?;";
        String getCount = "SELECT COUNT(*) FROM posts;";
        
        String requestForGetCurrentTime = "SELECT current_timestamp";
        String getDBNextValSeq = "SELECT nextval('posts_id_seq')";
        
        String updateCountPostsInForum = "UPDATE forums SET posts=posts+? WHERE slug=?;";
    }

    private Connection getConnectionDB() throws SQLException {
        return template.getDataSource().getConnection();
    }

    public void createTable() {
        template.execute(CommandStatic.createTable);
    }

    public void dropTable() {
        template.execute(CommandStatic.dropTable);
    }
    public void truncateTable() {
        template.execute(CommandStatic.truncateTable);
    }
    
    public int getCount() {
        return template.queryForObject(CommandStatic.getCount, Integer.class);
    }
    
    public Post getPostById(int id) {
        Post posts = null;
        try {
            posts = template.queryForObject(CommandStatic.getById, postMapper, id);
        } catch (Exception e) {}

        return posts;
    }

    public Post update(PostUpdate needPost, int needId) {
        if (needPost.getMessage() == null)
            return getPostById(needId);

        try {
            Post post = getPostById(needId);
            if (post.getMessage().equals(needPost.getMessage())) {
                return post;
            }

            template.update(CommandStatic.update, needPost.getMessage(), needId);
        } catch (DataAccessException | NullPointerException e) {
            return null;
        }
        return getPostById(needId);
    }

    public PostWithMarker flatSort(int threadId, Integer limit, Integer offset, Boolean desc) {
        String query = null;
        if (desc) {
            query = CommandStatic.selectFlatSortDesc;
        } else
            query = CommandStatic.selectFlatSort;

        List<Post> posts = null;
        try {
            posts = template.query(query, postMapper, threadId, limit, offset);
        } catch (DataAccessException e) {
            return null;
        }

        Integer newMarker = offset;
        if (!posts.isEmpty()) {
            newMarker += posts.size();
        }

        return new PostWithMarker(String.valueOf(newMarker), posts);
    }

    private PostWithMarker treeSort(int threadId, Integer limit, Integer offset, Boolean desc) {
        List<Post> posts = null;
        try {
            List<Map<String, Object>> rows;
            if(desc)
                posts = template.query(CommandStatic.selectTreeSortDesc, postMapper, threadId, limit, offset);
            else
                posts = template.query(CommandStatic.selectTreeSort, postMapper, threadId, limit, offset);

        } catch (DataAccessException e) {
            return null;
        }

        Integer newMarker = offset;
        if (!posts.isEmpty()) {
            newMarker += posts.size();
        }

        return new PostWithMarker(String.valueOf(newMarker), posts);
    }

    public PostWithMarker parentTreeSort(int threadId, Integer limit, Integer offset, Boolean desc) {
        ArrayList<Post> posts = null;

        int sizeParents;
        try {
            List<Map<String, Object>> parents;
            if(desc)
                parents = template.queryForList(CommandStatic.selectParentPostsDesc, threadId, limit, offset);
            else
                parents = template.queryForList(CommandStatic.selectParentPosts, threadId, limit, offset);

            sizeParents = parents.size();
            List<Post.PostId> postIds = Post.parseMapId(parents);

            posts = new ArrayList<>();

            String queryChooseChild;
            if(desc)
                queryChooseChild = CommandStatic.selectChildPostsDesc;
            else
                queryChooseChild = CommandStatic.selectChildPosts;

            for(Post.PostId parentPost : postIds) {
                int idParent = parentPost.getId();
                List<Post> childPost = Post.parseMap( template.queryForList(queryChooseChild, idParent) );
                for(Post child : childPost) {
                    posts.add(child);
                }
            }
        } catch (DataAccessException e) {
            return null;
        }

        return new PostWithMarker(String.valueOf(sizeParents + offset), posts);
    }

    public PostWithMarker getByThread(int threadId, Integer limit, String marker, String sort, Boolean desc) {
        if (desc == null)
            desc = false;

        if (marker == null)
            marker = "0";

        PostWithMarker page = null;

        if (sort == null || sort.equals("flat"))
            page = flatSort(threadId, limit, Integer.parseInt(marker), desc);
        else if (sort.equals("tree"))
            page = treeSort(threadId, limit, Integer.parseInt(marker), desc);
        else if (sort.equals("parent_tree"))
            page = parentTreeSort(threadId, limit, Integer.parseInt(marker), desc);

        return page;
    }

    public PreparedStatement generateAddPostStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
            CommandStatic.insertIntoPosts, Statement.NO_GENERATED_KEYS);
    }

    public void addPostBatch(PreparedStatement preparedStatement, Post post, Timestamp currentTime) throws SQLException {
        preparedStatement.setInt(1, post.getId());
        preparedStatement.setInt(2, post.getParent());
        preparedStatement.setString(3, post.getAuthor());
        preparedStatement.setString(4, post.getMessage());
        preparedStatement.setInt(5, post.getThread());
        preparedStatement.setString(6, post.getForum());

        preparedStatement.setInt(8, post.getParent());
        preparedStatement.setInt(9, post.getId());
        
        preparedStatement.setTimestamp(7, currentTime);
        preparedStatement.addBatch();
    }

    public List<Post> createPosts(List<Post> posts){
        List<Post> newPosts = new ArrayList<>();
        try {
            Connection connection = template.getDataSource().getConnection();
            
            PreparedStatement preparedAddPost = generateAddPostStatement(connection);
            PreparedStatement preparedAddUserInForum = userInForumService.generateStatement(connection);

            Map<String, Integer> updatedForums = new HashMap<>();
            Integer seq;
            Timestamp currentTime = null;
            String time = null;
            
            if(posts.get(0).getCreated() == null){
                currentTime = template.queryForObject(CommandStatic.requestForGetCurrentTime, Timestamp.class);
                time = TimeWork.getIsoTime(currentTime);
            }

            for (Post post: posts) {
                seq = template.queryForObject(CommandStatic.getDBNextValSeq, Integer.class);

                Timestamp nowTime;
                
                post.setId(seq);
                if(post.getCreated() == null) {
                    post.setCreated(time);
                    nowTime = currentTime;
                } else
                    nowTime = TimeWork.getTimeStampByUTC(post.getCreated());

                this.addPostBatch(preparedAddPost, post, nowTime);
                userInForumService.addBatch(preparedAddUserInForum,
                    post.getForum(),
                    post.getAuthor());

                if(!updatedForums.containsKey(post.getForum()))
                    updatedForums.put(post.getForum(), 1);
                else
                    updatedForums.put(post.getForum(), updatedForums.get(post.getForum()) + 1);

                newPosts.add(post);
            }

            try {
                MainService.endBatch(preparedAddPost);
            } catch (SQLException e) {
                /* if wrong author */
                preparedAddPost.close();
                preparedAddUserInForum.close();
                
                connection.close();
                
                return null;
            }
            
            preparedAddPost = connection.prepareStatement(CommandStatic.updateCountPostsInForum,
                Statement.NO_GENERATED_KEYS);
            for (Map.Entry<String, Integer> forum : updatedForums.entrySet()){
                preparedAddPost.setInt(1, forum.getValue());
                preparedAddPost.setString(2, forum.getKey());
                preparedAddPost.addBatch();
            }
            MainService.endBatch(preparedAddPost);
    
            batchManagerService.addPreparedStatement(connection, preparedAddUserInForum);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return newPosts;
    }

    private final RowMapper<Post> postMapper = (rs, num) ->
            new Post(rs.getInt("id"),
                rs.getInt("parent"),
                rs.getString("author"),
                rs.getString("message"),
                rs.getBoolean("isEdited"),
                rs.getString("forum"),
                rs.getInt("thread_id"),
                TimeWork.getIsoTime(rs.getTimestamp("created"))
            );
}
