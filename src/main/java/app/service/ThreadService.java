package app.service;

import app.models.ServiceAnswer;
import app.models.Thread;
import app.models.ThreadUpdate;
import app.util.Status;
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
import java.util.Map;


@Service
public class ThreadService {

    final private JdbcTemplate template;

    @Autowired
    public ThreadService(JdbcTemplate template){
        this.template = template;
    }

    void createTable(){
        String query = new StringBuilder()
                .append("CREATE EXTENSION IF NOT EXISTS citext; ")
                .append("CREATE TABLE IF NOT EXISTS threads ( ")
                .append("id SERIAL PRIMARY KEY, ")
                .append("title TEXT NOT NULL, ")
                .append("author CITEXT NOT NULL, ")
                .append("forum CITEXT NOT NULL, ")
                .append("message TEXT NOT NULL, ")
                .append("votes BIGINT NOT NULL DEFAULT 0, ")
                .append("slug CITEXT UNIQUE, ")
                .append("created TIMESTAMP NOT NULL DEFAULT current_timestamp, ")
                .append("FOREIGN KEY (author) REFERENCES users(nickname), ")
                .append("FOREIGN KEY (forum) REFERENCES forums(slug)); ")
                .append("SELECT setval('threads_id_seq',1);")
                .toString();

        template.execute(query);
    }

    void dropTable(){
        String query = new StringBuilder()
                .append("DROP TABLE IF EXISTS threads ;").toString();

        template.execute(query);
    }

    public ServiceAnswer<Thread> createNewThread(Thread thread){
        String query = new StringBuilder()
                .append("INSERT INTO threads(title, author, forum, message) ")
                .append("VALUES(?,?,?,?) RETURNING id;")
                .toString();
        String createdQuery = new StringBuilder()
                .append("UPDATE threads SET created = ? WHERE id = ? ;")
                .toString();
        String slugQuery = new StringBuilder()
                .append("UPDATE threads SET slug = ? WHERE id = ? ;")
                .toString();
        String subQuery = new StringBuilder()
                .append("UPDATE forums SET threads = threads + 1 ")
                .append("WHERE slug = ? ;")
                .toString();
        try {

            int id = template.queryForObject(query, Integer.class, thread.getTitle(), thread.getAuthor(), thread.getForum(), thread.getMessage());
            template.update(subQuery, thread.getForum());

            if(thread.getCreated() != null) {
                String st = ZonedDateTime.parse(thread.getCreated()).format(DateTimeFormatter.ISO_INSTANT);
                template.update(createdQuery, new Timestamp(ZonedDateTime.parse(st).toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli()), id);
            }

            if(thread.getSlug() != null) {
                template.update(slugQuery, thread.getSlug(), id);
            }
            thread.setId(id);
        }
        catch (DuplicateKeyException e){
            return new ServiceAnswer<>(getThreadBySlug(thread.getSlug()), Status.DUPLICATE);
        }

        return new ServiceAnswer<>(thread, Status.OK);
    }


    public Thread getThreadById(int id) {
        String query = String.format("SELECT * FROM threads WHERE id = '%d';", id);
        try {
            return template.queryForObject(query, threadMapper);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Thread getThreadBySlug(String slug) {
        String query = String.format("SELECT * FROM threads WHERE slug = '%s';", slug);
        try {
            return template.queryForObject(query, threadMapper);
        } catch (DataAccessException e) {
            //System.out.println(e.getMessage());
        }
        return null;
    }



    int getCount(){
        String query = new StringBuilder()
                .append("SELECT COUNT(*) FROM threads ;").toString();

        return template.queryForObject(query, Integer.class);
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


    public List<Thread> getThreadsByForum(String slug, Integer limit, String since, Boolean desc) {
        Timestamp time = null;
        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT * FROM threads WHERE forum = ? ");

        if(since != null) {
            String st = ZonedDateTime.parse(since).format(DateTimeFormatter.ISO_INSTANT);
            time = new Timestamp(ZonedDateTime.parse(st).toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        if(desc == null){
            desc = false;
        }

        if(since != null) {
            if (desc) {
                queryBuilder.append("AND created <= ? ");
            } else
                queryBuilder.append("AND created >= ? ");
        }

        if(desc) {
            queryBuilder.append("ORDER BY created DESC ");
        } else
            queryBuilder.append("ORDER BY created ");

        queryBuilder.append("LIMIT ? ;");

        String query = queryBuilder.toString();

        ArrayList<Thread> threads = null;
        try {
            List<Map<String, Object>> rows;
            if(since != null)
                rows = template.queryForList(query, slug, time, limit);
            else
                rows = template.queryForList(query, slug, limit);

            threads = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                threads.add(new Thread(
                                Integer.parseInt(row.get("id").toString()), row.get("title").toString(),
                                row.get("author").toString(), row.get("forum").toString(),
                                row.get("message").toString(), Integer.parseInt(row.get("votes").toString()),
                                row.get("slug").toString(), Timestamp.valueOf(row.get("created").toString())
                                .toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )
                );
            }

            return threads;
        }
        catch (DataAccessException e){}
        return null;
    }

    Thread resetVotes(int id){
        String query = new StringBuilder()
                .append("UPDATE threads SET votes = ( ")
                .append("SELECT SUM(voice) FROM votes WHERE thread_id = ? GROUP BY thread_id ) ")
                .append("WHERE id = ? ")
                .append("RETURNING * ;")
                .toString();

        try {
            return template.queryForObject(query, threadMapper, id, id);
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
}
