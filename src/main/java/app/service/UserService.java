package app.service;

import app.models.ServiceAnswer;
import app.models.User;
import app.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Transactional
public class UserService {

    final private JdbcTemplate template;

    @Autowired
    public UserService(JdbcTemplate template) {
        this.template = template;
    }

    public void createTable() {
        String query = new StringBuilder()
                .append("CREATE EXTENSION IF NOT EXISTS citext; ")
                .append("CREATE TABLE IF NOT EXISTS users ( ")
                .append("nickname CITEXT UNIQUE NOT NULL PRIMARY KEY, ")
                .append("fullname TEXT NOT NULL, ")
                .append("about TEXT NOT NULL, ")
                .append("email CITEXT UNIQUE NOT NULL); ")
                .toString();

        template.execute(query);
    }

    public void dropTable() {
        String query = new StringBuilder()
                .append("DROP TABLE IF EXISTS users;").toString();

        template.execute(query);
    }


    public int createNewUser(User user) {
        String query = new StringBuilder()
                .append("INSERT INTO users(about, email, fullname, nickname) VALUES(?,?,?,?) ;").toString();

        try {
            template.update(query, user.getAbout(), user.getEmail(), user.getFullname(), user.getNickname());
        } catch (DuplicateKeyException e) {
            return Status.DUPLICATE;
        } catch (DataAccessException e) {
            return Status.UNDEFINED;
        }

        return Status.OK;
    }

    public ArrayList<User> getDuplicates(User user) {
        String query = String.format("SELECT * FROM users WHERE nickname = '%s' OR email = '%s';",
                user.getNickname(), user.getEmail());
        try {
            List<Map<String, Object>> results = template.queryForList(query);
            ArrayList<User> duplicates = new ArrayList<>();
            for(Map<String, Object> res: results){
                duplicates.add(new User(
                        res.get("nickname").toString(), res.get("fullname").toString(),
                        res.get("about").toString(), res.get("email").toString()
                ));
            }
            return duplicates;
        } catch (DataAccessException e) {}

        return null;
    }

    public User getUserByNick(String nickname) {
        String query = String.format("SELECT * FROM users WHERE nickname = '%s';",
                nickname);
        try {
            User user = template.queryForObject(query, userMapper);
            if (user.isNotNull())
                return user;
        } catch (DataAccessException e) {}
        return null;
    }

    public ServiceAnswer<User> updateUser(User newData) {
        User user = getUserByNick(newData.getNickname());

        if (user == null)
            return new ServiceAnswer<>(null, Status.UNDEFINED);

        String queryEmail = String.format("SELECT * FROM users WHERE email = '%s' AND nickname != '%s';",
                newData.getEmail(), newData.getNickname());
        List<Map<String, Object>> results;
        try {
            results = template.queryForList(queryEmail);
        } catch (DataAccessException e) {
            System.out.println(e.getMessage());
            return new ServiceAnswer<>(null, Status.UNDEFINED);
        }

        if (!results.isEmpty())
            return new ServiceAnswer<>(null, Status.DUPLICATE);

        if (newData.getAbout() != null)
            user.setAbout(newData.getAbout());
        if (newData.getEmail() != null)
            user.setEmail(newData.getEmail());
        if (newData.getFullname() != null)
            user.setFullname(newData.getFullname());

        String queryUpdate = String.format("UPDATE users SET fullname = '%s', email = '%s', about = '%s' WHERE nickname='%s';",
                user.getFullname(),
                user.getEmail(),
                user.getAbout(),
                user.getNickname());
        try {
            template.execute(queryUpdate);
            return new ServiceAnswer<>(user, Status.OK);
        } catch (DataAccessException e) {
            return new ServiceAnswer<>(null, Status.UNDEFINED);
        }

    }

    public int getCount() {
        String query = new StringBuilder()
                .append("SELECT COUNT(*) FROM users ;").toString();

        return template.queryForObject(query, Integer.class);
    }


    public List<User> getUsersByForum(String slug, Integer limit, String since, Boolean desc) {

        StringBuilder queryBuilder = new StringBuilder()
                .append("SELECT * FROM users WHERE nickname IN (")
                .append("SELECT author FROM posts WHERE forum = ? UNION ")
                .append("SELECT author FROM threads WHERE forum = ?) ");

        if(desc == null){
            desc = false;
        }
        if(since != null) {
            if (desc) {
                queryBuilder.append("AND LOWER(nickname COLLATE \"ucs_basic\") < LOWER(? COLLATE \"ucs_basic\") ");
            } else
                queryBuilder.append("AND LOWER(nickname COLLATE \"ucs_basic\") > LOWER(? COLLATE \"ucs_basic\") ");
        }

        if(desc) {
            queryBuilder.append("ORDER BY LOWER(nickname COLLATE \"ucs_basic\") DESC ");
        } else
            queryBuilder.append("ORDER BY LOWER(nickname COLLATE \"ucs_basic\") ");

        queryBuilder.append("LIMIT ? ;");

        String query = queryBuilder.toString();

        ArrayList<User> users = null;
        try {
            List<Map<String, Object>> rows;
            if(since != null)
                rows = template.queryForList(query, slug, slug, since, limit);
            else
                rows = template.queryForList(query, slug, slug, limit);
            users = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                users.add(new User(
                                row.get("nickname").toString(), row.get("fullname").toString(),
                                row.get("about").toString(), row.get("email").toString()
                        )
                );
            }
            return users;
        }
        catch (DataAccessException e){}
        return null;
    }

    public boolean userExists(String nickname) {
        String query = String.format("SELECT COUNT(*) FROM users WHERE nickname = '%s';", nickname);
        return template.queryForObject(query, Integer.class) != 0;
    }

    private final RowMapper<User> userMapper = (rs, num) -> {
        final String nickname = rs.getString("nickname");
        final String fullname = rs.getString("fullname");
        final String email = rs.getString("email");
        final String about = rs.getString("about");

        return new User(nickname, fullname, about, email);
    };


}
