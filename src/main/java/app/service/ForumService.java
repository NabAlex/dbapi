package app.service;

import app.util.ResultPack;
import app.models.Forum;
import app.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;


@Service
public class ForumService {
    @Autowired
    private JdbcTemplate template;

    interface CommandStatic {
        String createTable = "CREATE EXTENSION IF NOT EXISTS citext; " +
            "CREATE TABLE IF NOT EXISTS forums ( " +
            "title TEXT NOT NULL, " +
            "creator CITEXT NOT NULL, " +
            "slug CITEXT UNIQUE NOT NULL PRIMARY KEY, " +
            "posts BIGINT NOT NULL DEFAULT 0, " +
            "threads BIGINT NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (creator) REFERENCES users(nickname)); ";


        String dropTable = "DROP TABLE IF EXISTS forums;";
        String truncateTable = "TRUNCATE TABLE forums CASCADE;";
        
        String insertIntoForum = "INSERT INTO forums(title, creator, slug) VALUES(?,?,?);";

        String selectBySlug = "SELECT * FROM forums WHERE slug=?;";
        String getCount = "SELECT COUNT(*) FROM forums;";
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


    public ResultPack<Forum> create(Forum forum) {
        try {
            template.update(CommandStatic.insertIntoForum, forum.getTitle(), forum.getUser(), forum.getSlug());
        } catch (DuplicateKeyException e) {
            return new ResultPack<>(getBySlug(forum.getSlug()), Status.CONFLICT);
        } catch (DataAccessException e) {
            return new ResultPack<>(null, Status.NOTFOUND);
        }
        return new ResultPack<>(forum, Status.OK);
    }


    public Forum getBySlug(String slug) {
        try {
            return template.queryForObject(CommandStatic.selectBySlug,
                forumMapper,
                slug);
        } catch (Exception e) {}

        return null;
    }

    public int getCount(){
        return template.queryForObject(CommandStatic.getCount, Integer.class);
    }

    private final RowMapper<Forum> forumMapper = (rs, num) ->
            new Forum(rs.getString("title"),
                rs.getString("creator"),
                rs.getString("slug"),
                rs.getInt("posts"),
                rs.getInt("threads")
            );
}
