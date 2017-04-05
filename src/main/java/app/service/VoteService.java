package app.service;

import app.models.Vote;
import app.models.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoteService {

    final private JdbcTemplate template;
    final private UserService userService;
    final private ForumService forumService;
    final private ThreadService threadService;
    final private PostService postService;

    @Autowired
    public VoteService(JdbcTemplate template, UserService userService,
                       ForumService forumService, ThreadService threadService,
                       PostService postService) {

        this.template = template;
        this.userService = userService;
        this.forumService = forumService;
        this.threadService = threadService;
        this.postService = postService;
    }

    public void createTable() {
        String createQuery = new StringBuilder()
                .append("CREATE TABLE IF NOT EXISTS votes ( ")
                .append("author CITEXT NOT NULL, ")
                .append("thread_id BIGINT NOT NULL, ")
                .append("voice INT NOT NULL, ")
                .append("FOREIGN KEY (author) REFERENCES users(nickname), ")
                .append("FOREIGN KEY (thread_id) REFERENCES threads(id)); ")
                .toString();
        String indexQuery = new StringBuilder()
                .toString();

        template.execute(createQuery);
        template.execute(indexQuery);
    }

    public void dropTable() {
        String query = new StringBuilder()
                .append("DROP TABLE IF EXISTS votes;").toString();

        template.execute(query);
    }


    public Thread createThreadVote(Vote vote, int thread_id) {
        String checkQuery = new StringBuilder()
                .append("SELECT COUNT(*) FROM votes ")
                .append("WHERE author = ? AND thread_id = ? ;")
                .toString();
        String updateQuery = new StringBuilder()
                .append("UPDATE votes ")
                .append("SET voice = ? ")
                .append("WHERE author = ? AND thread_id = ? ;")
                .toString();
        String insertQuery = new StringBuilder()
                .append("INSERT INTO votes (author, thread_id, voice) ")
                .append("VALUES(?,?,?) ;")
                .toString();

        try {
            if (template.queryForObject(checkQuery, Integer.class, vote.getNickname(), thread_id) > 0) {
                template.update(updateQuery, vote.getVoice(), vote.getNickname(), thread_id);
            } else
                template.update(insertQuery, vote.getNickname(), thread_id, vote.getVoice());
        } catch (DataAccessException e) {
            return null;
        }
        return threadService.resetVotes(thread_id);
    }

}
