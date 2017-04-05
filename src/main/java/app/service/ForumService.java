package app.service;

import app.models.ServiceAnswer;
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

    final private JdbcTemplate template;

    @Autowired
    public ForumService(JdbcTemplate template) {
        this.template = template;
    }

    public void createTable() {
        String query = new StringBuilder()
                .append("CREATE EXTENSION IF NOT EXISTS citext; ")
                .append("CREATE TABLE IF NOT EXISTS forums ( ")
                .append("title TEXT NOT NULL, ")
                .append("creator CITEXT NOT NULL, ")
                .append("slug CITEXT UNIQUE NOT NULL PRIMARY KEY, ")
                .append("posts BIGINT NOT NULL DEFAULT 0, ")
                .append("threads BIGINT NOT NULL DEFAULT 0, ")
                .append("FOREIGN KEY (creator) REFERENCES users(nickname)); ")
                .toString();

        template.execute(query);
    }

    public void dropTable() {
        String query = new StringBuilder()
                .append("DROP TABLE IF EXISTS forums ;").toString();

        template.execute(query);
    }


    public ServiceAnswer<Forum> createNewForum(Forum forum) {
        String query = new StringBuilder()
                .append("INSERT INTO forums(title, creator, slug) VALUES(?,?,?) ;")
                .toString();

        try {
            template.update(query, forum.getTitle(), forum.getUser(), forum.getSlug());
        } catch (DuplicateKeyException e) {
            //System.out.println(e.getMessage());
            return new ServiceAnswer<>(getForumBySlug(forum.getSlug()), Status.DUPLICATE);
        } catch (DataAccessException e) {
            //System.out.println(e.getMessage());
            return new ServiceAnswer<>(null, Status.UNDEFINED);
        }

        return new ServiceAnswer<>(forum, Status.OK);
    }


    public Forum getForumBySlug(String slug) {
        String query = String.format("SELECT * FROM forums WHERE slug = '%s';", slug);
        try {
            return template.queryForObject(query, forumMapper);
        } catch (DataAccessException e) {
            //System.out.println(e.getMessage());
        }
        return null;
    }


    private final RowMapper<Forum> forumMapper = (rs, num) -> {
        final String title = rs.getString("title");
        final String user = rs.getString("creator");
        final String slug = rs.getString("slug");
        final int posts = rs.getInt("posts");
        final int threads = rs.getInt("threads");

        return new Forum(title, user, slug, posts, threads);
    };

    public int getCount(){
        String query = new StringBuilder()
                .append("SELECT COUNT(*) FROM forums ;").toString();

        return template.queryForObject(query, Integer.class);
    }
}
