package app.service;

import app.Application;
import app.models.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
@Transactional
public class MainService {
    JdbcTemplate template;

    private PostService postService;
    private VoteService voteService;
    private ForumService forumService;
    private UserService userService;
    private ThreadService threadService;
    private UserInForumService userInForumService;

    // Create index on user email lower

    // create index parentTreeSort and flatSort

    // add user_forum for getUsersByForum ()
    interface CommandStatic {
        String createExtention = "CREATE EXTENSION IF NOT EXISTS CITEXT;";

        String createIndexForumSlug = "CREATE INDEX ON forums (lower(slug));";
        
        String createNeedIndexUsers = "CREATE INDEX ON users(lower(nickname COLLATE \"ucs_basic\"));";
        
        String createNeedIndexPosts = "CREATE INDEX ON posts(forum);" +
            "CREATE INDEX ON posts(author);" +
            "CREATE index on posts(thread_id, parent, id);" +
            "CREATE index on posts(thread_id, id);" +
            "CREATE INDEX ON posts((post_path[1]), id);" +
            "CREATE INDEX ON posts(thread_id, post_path);";
        
        String createNeedIndexVotes = "CREATE INDEX ON votes(author, thread_id);";
        
        String createNeedIndexThread = "CREATE INDEX ON threads(slug);" +
            "CREATE INDEX ON threads(lower(forum));";
        
        String createNeedIndexUserInForum = "CREATE INDEX ON user_in_forum (forum, user_name);" +
            "CREATE INDEX ON user_in_forum (user_name);" +
            "CREATE INDEX ON user_in_forum (forum);";
    }

    public MainService(@Autowired JdbcTemplate template,
                       @Autowired PostService postService,
                       @Autowired VoteService voteService,
                       @Autowired ForumService forumService,
                       @Autowired UserService userService,
                       @Autowired ThreadService threadService,
                       @Autowired UserInForumService userInForumService) {
        this.template = template;

        this.userService = userService;
        this.forumService = forumService;
        this.threadService = threadService;

        this.postService = postService;

        this.voteService = voteService;
        this.userInForumService = userInForumService;

        if(Application.localDebugMode)
            this.dropTables();

        this.createTables();
    }

    public void createIndexes() {
        template.update(CommandStatic.createIndexForumSlug);
        
        template.update(CommandStatic.createNeedIndexPosts);
        template.update(CommandStatic.createNeedIndexThread);
        template.update(CommandStatic.createNeedIndexUsers);
        template.update(CommandStatic.createNeedIndexVotes);
        template.update(CommandStatic.createNeedIndexUserInForum);
    }
    
    public void dropTables() {
        userInForumService.dropTable();
        voteService.dropTable();
        postService.dropTable();
        threadService.dropTable();
        forumService.dropTable();
        userService.dropTable();
    }
    
    public void truncateTable() {
        userInForumService.truncateTable();
        voteService.truncateTable();
        postService.truncateTable();
        threadService.truncateTable();
        forumService.truncateTable();
        userService.truncateTable();
    }

    public void createTables() {
        template.update(CommandStatic.createExtention);

        userService.createTable();
        forumService.createTable();
        threadService.createTable();
        postService.createTable();
        voteService.createTable();
        userInForumService.createTable();

        createIndexes();
    }

    public Status getStatus(){
        return new Status(userService.getCount(),
            forumService.getCount(),
            threadService.getCount(),
            postService.getCount()
        );
    }

    public static void endBatch(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.executeBatch();
        preparedStatement.close();
    }
}
