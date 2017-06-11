package app.service;

import app.util.ResultPack;
import app.models.Thread;
import app.models.ThreadUpdate;
import app.util.Status;
import app.util.TimeWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;


@Service
public class ThreadService {
    @Autowired
    private JdbcTemplate template;

    @Autowired
    private UserInForumService userInForumService;

    interface CommandStatic {
        String createTable = "CREATE EXTENSION IF NOT EXISTS citext; " +
            "CREATE TABLE IF NOT EXISTS threads ( " +
            "id SERIAL PRIMARY KEY, " +
            "title TEXT NOT NULL, " +
            "author CITEXT NOT NULL, " +
            "forum CITEXT NOT NULL, " +
            "message TEXT NOT NULL, " +
            "votes BIGINT NOT NULL DEFAULT 0, " +
            "slug CITEXT UNIQUE, " +
            "created TIMESTAMP NOT NULL DEFAULT current_timestamp, " +
            "FOREIGN KEY (author) REFERENCES users(nickname), " +
            "FOREIGN KEY (forum) REFERENCES forums(slug)); " +
            "SELECT setval('threads_id_seq',1);";

        String dropTable = "DROP TABLE IF EXISTS threads;";
        String truncateTable = "TRUNCATE TABLE threads CASCADE;";
        
        String insertIntoThread = "INSERT INTO threads(title, author, forum, message,created,slug) " +
            "VALUES(?,?,?,?,DEFAULT,DEFAULT) RETURNING id;";
        String insertIntoThreadWithSlug = "INSERT INTO threads(title, author, forum, message,created,slug) " +
            "VALUES(?,?,?,?,DEFAULT,?) RETURNING id;";
        String insertIntoThreadWithCreated = "INSERT INTO threads(title, author, forum, message,created,slug) " +
            "VALUES(?,?,?,?,?,DEFAULT) RETURNING id;";
        String insertIntoThreadWithCreatedAndSlug = "INSERT INTO threads(title, author, forum, message,created,slug) " +
            "VALUES(?,?,?,?,?,?) RETURNING id;";

        String updateForum = "UPDATE forums SET threads = threads + 1 " +
            "WHERE slug = ? ;";

        String updateVoteAndReturn = "UPDATE threads SET votes=? WHERE id=? RETURNING *;";

        String getThreadById = "SELECT * FROM threads WHERE id = ?";
        String getThreadBySlug = "SELECT * FROM threads WHERE slug = ?;";

        String checkThreadById = "SELECT id FROM threads WHERE id = ?;";
        String checkThreadBySlug = "SELECT id FROM threads WHERE slug = ?;";

        String getCount = "SELECT COUNT(*) FROM threads;";
    
        String ImplselectBaseByForum = "SELECT * FROM threads WHERE forum = ?";
        String selectBaseByForumDescWithCreated = ImplselectBaseByForum +
            " AND created <= ?" +
            " ORDER BY created DESC LIMIT ?;";
    
        String selectBaseByForumDesc = ImplselectBaseByForum +
            " ORDER BY created DESC LIMIT ?;";
    
        String selectBaseByForumWithCreated = ImplselectBaseByForum +
            " AND created >= ?" +
            " ORDER BY created LIMIT ?;";
    
        String selectBaseByForum = ImplselectBaseByForum +
            " ORDER BY created LIMIT ?;";

        // String updateCreatedAnd
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
    
    public ResultPack<Thread> createNewThread(Thread thread){
        int id;
        try {
            String created = thread.getCreated();
            String slug = thread.getSlug();

            if(created != null && slug != null) {
                id = template.queryForObject(CommandStatic.insertIntoThreadWithCreatedAndSlug,
                    Integer.class,
                    thread.getTitle(),
                    thread.getAuthor(),
                    thread.getForum(),
                    thread.getMessage(),
                    TimeWork.toZonedDateTime(created),
                    slug);
            } else if(created != null) {
                id = template.queryForObject(CommandStatic.insertIntoThreadWithCreated,
                    Integer.class,
                    thread.getTitle(),
                    thread.getAuthor(),
                    thread.getForum(),
                    thread.getMessage(),
                    TimeWork.toZonedDateTime(created));
            } else if(slug != null) {
                id = template.queryForObject(CommandStatic.insertIntoThreadWithSlug,
                    Integer.class,
                    thread.getTitle(),
                    thread.getAuthor(),
                    thread.getForum(),
                    thread.getMessage(),
                    slug);
            } else {
                id = template.queryForObject(CommandStatic.insertIntoThread,
                    Integer.class,
                    thread.getTitle(),
                    thread.getAuthor(),
                    thread.getForum(),
                    thread.getMessage());
            }
            thread.setId(id);

            userInForumService.addUserToForum(thread.getForum(), thread.getAuthor());
            template.update(CommandStatic.updateForum, thread.getForum());
        }
        catch (DuplicateKeyException e){
            return new ResultPack<>(getThreadBySlug(thread.getSlug()), Status.CONFLICT);
        }

        return new ResultPack<>(thread, Status.OK);
    }


    public Thread getThreadById(int id) {
        try {
            return template.queryForObject(CommandStatic.getThreadById, threadMapper, id);
        } catch (DataAccessException e) {}

        return null;
    }

    public Integer checkThreadById(int id) {
        try {
            return template.queryForObject(CommandStatic.checkThreadById, Integer.class, id);
        } catch (DataAccessException e) {}

        return null;
    }

    public Integer checkThreadBySlug(String slug) {
        try {
            return template.queryForObject(CommandStatic.checkThreadBySlug, Integer.class, slug);
        } catch (DataAccessException e) {}

        return null;
    }

    public Thread getThreadBySlug(String slug) {
        try {
            return template.queryForObject(CommandStatic.getThreadBySlug, threadMapper, slug);
        } catch (DataAccessException e) {}
        return null;
    }

    int getCount() {
        return template.queryForObject(CommandStatic.getCount, Integer.class);
    }
    
    public List<Thread> getThreadsByForum(String slug, Integer limit, String since, Boolean desc) {
        Timestamp time = null;
        if(since != null)
            time = TimeWork.getTimeStampByUTC(since);

        if(desc == null)
            desc = false;

        String query;
        if(since != null) {
            if (desc) {
                query = CommandStatic.selectBaseByForumDescWithCreated;
            } else
                query = CommandStatic.selectBaseByForumWithCreated;
        } else {
            if(desc)
                query = CommandStatic.selectBaseByForumDesc;
            else
                query = CommandStatic.selectBaseByForum;
        }
        
        ArrayList<Thread> threads = null;
        try {
            List<Map<String, Object>> rows;
            if(since != null)
                rows = template.queryForList(query, slug, time, limit);
            else
                rows = template.queryForList(query, slug, limit);

            threads = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                threads.add(Thread.getThreadByRow(row));
            }

            return threads;
        }
        catch (DataAccessException e){}

        return null;
    }

    Thread changeVote(int threadId, int change){
        try {
            return template.queryForObject(CommandStatic.updateVoteAndReturn, threadMapper,
                change, threadId);
        } catch (DataAccessException e) {}

        return null;
    }

    public Thread updateThread(ThreadUpdate threadUpdate, int id){
        StringBuilder queryBuilder = new StringBuilder()
                .append("UPDATE threads SET ");

        boolean oneUpdated = false;
        if(threadUpdate.getTitle() != null){
            queryBuilder.append("title = '" + threadUpdate.getTitle() + "',");
            oneUpdated = true;
        }
        if(threadUpdate.getMessage() != null){
            queryBuilder.append("message = '" + threadUpdate.getMessage() + "',");
            oneUpdated = true;
        }
        queryBuilder.deleteCharAt(queryBuilder.length()-1);
        queryBuilder.append(" WHERE id = '" + id + "' ;");

        if(oneUpdated) {
            try {
                template.update(queryBuilder.toString());
            } catch (DataAccessException e) {
                return null;
            }
        }
        return this.getThreadById(id);
    }

    private final RowMapper<Thread> threadMapper = (rs, num) -> {
        final int id = rs.getInt("id");
        final String title = rs.getString("title");
        final String author = rs.getString("author");
        final String forum = rs.getString("forum");
        final String message = rs.getString("message");
        final int votes = rs.getInt("votes");
        final String slug = rs.getString("slug");
        final String created = rs.getTimestamp("created").toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new Thread(id, title, author, forum, message, votes, slug, created);
    };
}
