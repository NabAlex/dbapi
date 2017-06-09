package app.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UpdateVoteService {
    private static int timeFlush = 3000; /* TODO update */

    final private Map<Integer, Integer> voteStatus = new ConcurrentHashMap<>();
    final private Map<String, Map<Integer, Integer>> userTreadVote = new ConcurrentHashMap<>();

    public boolean existsInCache(String nickname) {
        return userTreadVote.containsKey(nickname);
    }

    Integer changeSlugVoteAndReturn(int slugId, int newVote, String nickName) {
        int oldValue = 0;
        if (userTreadVote.containsKey(nickName)) {
            Map<Integer, Integer> tempUserState = userTreadVote.get(nickName);
            if (tempUserState.containsKey(slugId)) {
                oldValue = tempUserState.get(slugId);
                if (oldValue == newVote)
                    return null;
            }
        } else
            userTreadVote.put(nickName, new HashMap<>());

        int finalValue = newVote;
        if (voteStatus.containsKey(slugId))
            finalValue = voteStatus.get(slugId) + newVote - oldValue;

        voteStatus.put(slugId, finalValue);
        userTreadVote.get(nickName).put(slugId, newVote);

        return finalValue;
    }




//    synchronized Integer changeSlugVoteAndReturn(int slugId, int newVote, String nickName) {
//        int finalValue = newVote;
//
//        if (userTreadVote.containsKey(nickName)) {
//            Map<Integer, Integer> tempUserState = userTreadVote.get(nickName);
//
////            if (tempUserState.containsKey(slugId)) {
////                int oldValue = tempUserState.get(slugId);
////                if (oldValue == newVote)
////                    return null;
////
////                finalValue -= oldValue;
////            }
//        } else
//            userTreadVote.put(nickName, new HashMap<>());
//
//        if (voteStatus.containsKey(slugId))
//            finalValue += voteStatus.get(slugId);
//
//        voteStatus.put(slugId, finalValue);
//        userTreadVote.get(nickName).put(slugId, newVote);
//
//        return finalValue;
//    }
}
