package app.service;

import app.models.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class ServiceService {

    final private JdbcTemplate template;
    final private UserService userService;
    final private ForumService forumService;
    final private ThreadService threadService;
    final private PostService postService;
    final private VoteService voteService;

    @Autowired
    public ServiceService(JdbcTemplate template,
                          UserService userService,
                          ForumService forumService,
                          ThreadService threadService,
                          PostService postService,
                          VoteService voteService){

        this.template = template;
        this.userService = userService;
        this.forumService = forumService;
        this.threadService = threadService;
        this.postService = postService;
        this.voteService = voteService;
        dropAllTables();
        createAllTables();
    }

    public void createAllTables(){
        userService.createTable();
        forumService.createTable();
        threadService.createTable();
        postService.createTable();
        voteService.createTable();
    }

    public void dropAllTables(){
        voteService.dropTable();
        postService.dropTable();
        threadService.dropTable();
        forumService.dropTable();
        userService.dropTable();
    }

    public Status getStatus(){
        return new Status(userService.getCount(), forumService.getCount(), threadService.getCount(), postService.getCount());
    }

}
