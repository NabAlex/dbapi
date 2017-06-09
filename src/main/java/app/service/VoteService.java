package app.service;

import app.models.Vote;
import app.models.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class VoteService {
    @Autowired
    private JdbcTemplate template;

    @Autowired
    private ThreadService threadService;

    @Autowired
    private UpdateVoteService updateVoteService;

    interface CommandStatic {
        String createTable = "CREATE TABLE IF NOT EXISTS votes ( " +
            "author CITEXT NOT NULL, " +
            "thread_id BIGINT NOT NULL, " +
            "voice INT NOT NULL, " +
            "UNIQUE (author, thread_id), " +
            "FOREIGN KEY (author) REFERENCES users(nickname), " +
            "FOREIGN KEY (thread_id) REFERENCES threads(id)); ";

        String dropTable = "DROP TABLE IF EXISTS votes;";
        String truncateTable = "TRUNCATE TABLE votes CASCADE;";

        String upsertVote = "INSERT INTO votes(author, thread_id, voice) VALUES (?, ?, ?)" +
            " ON CONFLICT (author, thread_id) DO UPDATE SET voice=?;";
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
    
    public Thread createThreadVote(Vote vote, int threadId) {
        try {
            template.update(CommandStatic.upsertVote, vote.getNickname(),
                threadId, vote.getVoice(), vote.getVoice());
        } catch (DataAccessException e) {
            return null;
        }

        Integer newVotes = updateVoteService.changeSlugVoteAndReturn(threadId,
            vote.getVoice(), vote.getNickname());

        if(newVotes == null)
            return threadService.getThreadById(threadId);

        return threadService.changeVote(threadId, newVotes);
    }

}
