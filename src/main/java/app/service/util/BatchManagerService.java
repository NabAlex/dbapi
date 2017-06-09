package app.service.util;

import app.service.MainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Transactional
public class BatchManagerService {
    private Thread localThread = null;
    
    class BatchData {
        Connection
    }
    final Queue<PreparedStatement> preparedStatementQueue =
        new ConcurrentLinkedDeque<>();
    
    public BatchManagerService() {
        localThread = new Thread(new BatchManagerThread());
        localThread.start();
    }
    
    public boolean addPreparedStatement(PreparedStatement preparedStatement) {
        return preparedStatementQueue.offer(preparedStatement);
    }
    
    class BatchManagerThread implements Runnable {
        @Override
        public void run() {
            try {
                PreparedStatement temp;
                while(true) {
                    temp = preparedStatementQueue.poll();
                    if(temp != null) {
                        System.out.println("execute batch! " + preparedStatementQueue.size());
                        MainService.endBatch(temp);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
