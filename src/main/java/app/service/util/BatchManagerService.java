package app.service.util;

import app.service.MainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Transactional
public class BatchManagerService {
    private BatchManagerThread localThread = null;
    
    class BatchData {
        Connection connection;
        PreparedStatement preparedStatement;
        
        BatchData(Connection connection, PreparedStatement preparedStatement) {
            this.connection = connection;
            this.preparedStatement = preparedStatement;
        }
    }
    
    private final Queue<BatchData> preparedStatementQueue =
        new ConcurrentLinkedDeque<>();
    
    public BatchManagerService() {
        localThread = new BatchManagerThread();
    }
    
    public void addPreparedStatement(Connection connection, PreparedStatement preparedStatement) {
        preparedStatementQueue.offer(new BatchData(connection, preparedStatement));
        
        if (localThread.isDone()) {
            localThread = new BatchManagerThread();
            localThread.start();
        }
    }
    
    class BatchManagerThread extends Thread {
        private volatile boolean done = true;
    
        synchronized boolean isDone() {
            return done;
        }
    
        private synchronized void setDone(boolean done) {
            this.done = done;
        }
    
        @Override
        public void run() {
            setDone(false);
            
            try {
                BatchData temp;
                while (true) {
                    temp = preparedStatementQueue.poll();
                    if (temp != null) {
                        MainService.endBatch(temp.preparedStatement);
                        temp.connection.close();
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            setDone(true);
        }
    }
}
