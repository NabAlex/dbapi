package app.controllers;

import app.models.*;
import app.models.Thread;
import app.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ThreadController {
    @Autowired UpdateVoteService updateVoteService;
    
    final private ThreadService threadService;
    final private PostService postService;
    final private UserService userService;
    final private VoteService voteService;

    @Autowired
    public ThreadController(ThreadService threadService, PostService postService, UserService userService, VoteService voteService){
        this.threadService = threadService;
        this.postService = postService;
        this.userService = userService;
        this.voteService = voteService;
    }

    @RequestMapping(path = "/api/thread/{slug_or_id}/create", method = RequestMethod.POST)
    public ResponseEntity createPost(@PathVariable(name = "slug_or_id") String slug_or_id,
                                     @RequestBody List<Post> posts){
        Thread thread;
        if(isNumeric(slug_or_id)) {
            thread = threadService.getThreadById(Integer.parseInt(slug_or_id));
        } else
            thread = threadService.getThreadBySlug(slug_or_id);

        if(thread == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        for (Post post: posts) {
            if(post.getParent() != 0) {
                Post parent = postService.getPostById(post.getParent());
                post.setForum(thread.getForum());
                post.setThread(thread.getId());
                if (parent == null || thread.getId() != parent.getThread()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }
            post.setForum(thread.getForum());
            post.setThread(thread.getId());
        }

        List<Post> newPosts = postService.createPosts(posts);
        if (newPosts == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newPosts);
    }

    @RequestMapping(path = "/api/thread/{slug_or_id}/details", method = RequestMethod.GET)
    public ResponseEntity detailsThread(@PathVariable(name = "slug_or_id") String slug_or_id){
        Thread thread;
        if(isNumeric(slug_or_id)) {
            thread = threadService.getThreadById(Integer.parseInt(slug_or_id));
        } else
            thread = threadService.getThreadBySlug(slug_or_id);

        if(thread == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(thread);
    }

    @RequestMapping(path = "/api/thread/{slug_or_id}/details", method = RequestMethod.POST)
    public ResponseEntity updateThread(@PathVariable(name = "slug_or_id") String slug_or_id,
                                       @RequestBody ThreadUpdate tup){
        Thread thread;
        if(isNumeric(slug_or_id)) {
            thread = threadService.getThreadById(Integer.parseInt(slug_or_id));
        } else
            thread = threadService.getThreadBySlug(slug_or_id);
        if(thread == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Thread updatedThread = threadService.updateThread(tup, thread.getId());
        return ResponseEntity.status(HttpStatus.OK).body(updatedThread);
    }

    @RequestMapping(path = "/api/thread/{slug_or_id}/vote", method = RequestMethod.POST)
    public ResponseEntity voteThread(@PathVariable(name = "slug_or_id") String slugOrId,
                                       @RequestBody Vote vote){
        Integer threadId;
        if(isNumeric(slugOrId)) {
            threadId = threadService.checkThreadById(Integer.parseInt(slugOrId));
        } else
            threadId = threadService.checkThreadBySlug(slugOrId);

        if(threadId == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String targetNickName = vote.getNickname();
        if(!updateVoteService.existsInCache(targetNickName) &&
           !userService.userExists(vote.getNickname()))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Thread votedThread = voteService.createThreadVote(vote, threadId);
        return ResponseEntity.status(HttpStatus.OK).body(votedThread);
    }

    @RequestMapping(path="/api/thread/{slug_or_id}/posts", method = RequestMethod.GET)
    public ResponseEntity postsThread(@PathVariable(name = "slug_or_id") String slug_or_id,
                                      @RequestParam(name = "limit", required = false) Integer limit,
                                      @RequestParam(name = "marker", required = false) String marker,
                                      @RequestParam(name = "sort", required = false) String sort,
                                      @RequestParam(name = "desc", required = false) Boolean desc){
        Thread thread;
        if(isNumeric(slug_or_id)) {
            thread = threadService.getThreadById(Integer.parseInt(slug_or_id));
        } else
            thread = threadService.getThreadBySlug(slug_or_id);

        if(thread == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PostWithMarker postsSorted =
            postService.getByThread(thread.getId(), limit, marker, sort, desc);

        return ResponseEntity.status(HttpStatus.OK).body(postsSorted);
    }

    // TODO hide
    private static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
}
