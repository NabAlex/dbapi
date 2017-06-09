package app.service;

import app.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserInForumService {
    @Autowired
    private JdbcTemplate template;

    interface CommandStatic {
        String createTable = "CREATE TABLE IF NOT EXISTS user_in_forum(" +
            "  forum CITEXT NOT NULL," +
            "  user_name CITEXT NOT NULL," +
            /*"  FOREIGN KEY (forum) REFERENCES forums(slug)," +
            "  FOREIGN KEY (user_name) REFERENCES users(nickname)," +*/
            "  UNIQUE (forum, user_name)" +
            ");";

        String dropTable = "DROP TABLE IF EXISTS user_in_forum;";
        String truncateTable = "TRUNCATE TABLE user_in_forum CASCADE;";
        
        String insertForumUser = "INSERT INTO user_in_forum (forum, user_name) VALUES (?, ?)" +
            " ON CONFLICT DO NOTHING;";

        String ImplSelectBySlugWithFilter = "SELECT u.email, u.fullname, u.nickname, u.about FROM user_in_forum AS uif\n" +
            "JOIN users AS u ON (uif.user_name = u.nickname)";

        String selectBySlugWithFilterMore = ImplSelectBySlugWithFilter +
            " WHERE forum = ? AND" +
            "  LOWER(user_name COLLATE \"ucs_basic\") > LOWER(? COLLATE \"ucs_basic\")" +
            "  ORDER BY LOWER(user_name COLLATE \"ucs_basic\")" +
            "  LIMIT ?;";
        
        String selectBySlugWithFilterLessDesc = ImplSelectBySlugWithFilter +
            " WHERE forum = ? AND" +
            "  LOWER(user_name COLLATE \"ucs_basic\") < LOWER(? COLLATE \"ucs_basic\")" +
            "  ORDER BY LOWER(user_name COLLATE \"ucs_basic\") DESC" +
            "  LIMIT ?;";
        
        String selectBySlug = ImplSelectBySlugWithFilter +
            " WHERE forum = ?" +
            "  ORDER BY LOWER(user_name COLLATE \"ucs_basic\")" +
            "  LIMIT ?;";
    
        String selectBySlugDesc = ImplSelectBySlugWithFilter +
            " WHERE forum = ?" +
            "  ORDER BY LOWER(user_name COLLATE \"ucs_basic\") DESC" +
            "  LIMIT ?;";
    }

    public void createTable() {
        template.execute(CommandStatic.createTable);
    }

    public void dropTable() {
        System.out.println("Why???");
        template.execute(CommandStatic.dropTable);
    }
    public void truncateTable() {
        template.execute(CommandStatic.truncateTable);
    }

    public void addUserToForum(String slugId, String nickname) {
        template.update(CommandStatic.insertForumUser,
            slugId,
            nickname);
    }

    public PreparedStatement generateStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(CommandStatic.insertForumUser,
            Statement.NO_GENERATED_KEYS);
    }

    public void addBatch(PreparedStatement preparedStatement, String slugId, String nickname) throws SQLException {
        preparedStatement.setString(1, slugId);
        preparedStatement.setString(2, nickname);
        preparedStatement.addBatch();
    }

    public List<User> selectUsersBySlugWithFilter(String slugForum, String filterNickname,
                                                  Integer limit, Boolean desc) {
        List<Map<String, Object>> rows;
    
        if(desc == null)
            desc = false;
        
        if(filterNickname == null) {
            if(desc)
                rows = template.queryForList(CommandStatic.selectBySlugDesc, slugForum, limit);
            else
                rows = template.queryForList(CommandStatic.selectBySlug, slugForum, limit);
        } else {
            if(desc)
                rows = template.queryForList(CommandStatic.selectBySlugWithFilterLessDesc,
                    slugForum, filterNickname, limit);
            else
                rows = template.queryForList(CommandStatic.selectBySlugWithFilterMore,
                    slugForum, filterNickname, limit);
        }
        
        return User.parseMap(rows);
    }
}
