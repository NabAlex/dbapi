package app.service;

import app.util.ResultPack;
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
    @Autowired
    private JdbcTemplate template;

    public UserService(JdbcTemplate template) {
        this.template = template;
    }

    interface CommandStatic {
        String createTable = "CREATE TABLE IF NOT EXISTS users ( " +
            "nickname CITEXT UNIQUE NOT NULL PRIMARY KEY, " +
            "fullname TEXT NOT NULL, " +
            "about TEXT NOT NULL, " +
            "email CITEXT UNIQUE NOT NULL); ";

        String dropTable = "DROP TABLE IF EXISTS users;";
        String truncateTable = "TRUNCATE TABLE users CASCADE;";
        
        String insertIntoUsers = "INSERT INTO users(about, email, fullname, nickname) VALUES(?,?,?,?) ;";
        String selectByNickAndEmail = "SELECT * FROM users WHERE nickname=? OR email=?;";
        String getSelectByNickAndEmailWithoutNick = "SELECT * FROM users WHERE (nickname=? OR email=?) AND nickname <> ?;";

        String selectUserByNickname = "SELECT * FROM users WHERE nickname=?;";

        String existsUser = "SELECT COUNT(*) FROM users WHERE nickname=?;";

        String updateUser = "UPDATE users " +
            "SET fullname=?, email=?, about=? WHERE nickname=?;";

        String getCount = "SELECT COUNT(*) FROM users ;";
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
    public int createNewUser(User user) {
        try {
            template.update(CommandStatic.insertIntoUsers,
                user.getAbout(),
                user.getEmail(),
                user.getFullname(),
                user.getNickname());

        } catch (DuplicateKeyException e) {
            return Status.CONFLICT;
        } catch (DataAccessException e) {
            return Status.NOTFOUND;
        }

        return Status.OK;
    }

    public List<User> getDuplicates(User user, String oldNick) {
        try {
            if(oldNick == null)
                return User.parseMap( template.queryForList(CommandStatic.selectByNickAndEmail,
                    user.getNickname(),
                    user.getEmail())
                );
            else
                return User.parseMap( template.queryForList(CommandStatic.getSelectByNickAndEmailWithoutNick,
                    user.getNickname(),
                    user.getEmail(),
                    oldNick)
                );
        } catch (DataAccessException e) {}

        return null;
    }

    public User getUserByNickname(String nickname) {
        try {
            User user = template.queryForObject(CommandStatic.selectUserByNickname,
                userMapper,
                nickname
            );

            /* ??? */
            if (User.isIsSet(user))
                return user;

        } catch (DataAccessException e) {}

        return null;
    }

    public ResultPack<User> updateUser(User newUserData) {
        User user = getUserByNickname(newUserData.getNickname());

        if (user == null)
            return new ResultPack<>(null, Status.NOTFOUND);


        List<User> usersDup = getDuplicates(newUserData, user.getNickname());
        if (usersDup != null && usersDup.size() > 0)
            return new ResultPack<>(null, Status.CONFLICT);

        if (newUserData.getAbout() != null)
            user.setAbout(newUserData.getAbout());
        if (newUserData.getEmail() != null)
            user.setEmail(newUserData.getEmail());
        if (newUserData.getFullname() != null)
            user.setFullname(newUserData.getFullname());

        try {
            template.update(CommandStatic.updateUser,
                user.getFullname(),
                user.getEmail(),
                user.getAbout(),
                user.getNickname()); /* where */

            return new ResultPack<>(user, Status.OK);
        } catch (DataAccessException e) {
            return new ResultPack<>(null, Status.NOTFOUND);
        }
    }

    public int getCount() {
        return template.queryForObject(CommandStatic.getCount, Integer.class);
    }

    public boolean userExists(String nickname) {
        return template.queryForObject(CommandStatic.existsUser, Integer.class, nickname) != 0;
    }

    private final RowMapper<User> userMapper = (rs, num) -> {
        final String nickname = rs.getString("nickname");
        final String fullname = rs.getString("fullname");
        final String email = rs.getString("email");
        final String about = rs.getString("about");

        return new User(nickname, fullname, about, email);
    };


}
