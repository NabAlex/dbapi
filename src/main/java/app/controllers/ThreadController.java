package app.controllers;

import app.service.PostService;
import app.service.ThreadService;
import app.service.UserService;
import app.service.VoteService;
import app.models.Post;
import app.models.Thread;
import app.models.ThreadUpdate;
import app.models.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/thread")
public class ThreadController {

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

    @RequestMapping(path = "/{slug_or_id}/create", method = RequestMethod.POST)
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

        for (Post post: posts){
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

        List<Post> newPosts = postService.createManyPosts(posts);
        if (newPosts == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newPosts);
    }

    @RequestMapping(path = "/{slug_or_id}/details", method = RequestMethod.GET)
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

    @RequestMapping(path = "/{slug_or_id}/details", method = RequestMethod.POST)
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

    @RequestMapping(path = "/{slug_or_id}/vote", method = RequestMethod.POST)
    public ResponseEntity voteThread(@PathVariable(name = "slug_or_id") String slug_or_id,
                                       @RequestBody Vote vote){
        Thread thread;
        if(isNumeric(slug_or_id)) {
            thread = threadService.getThreadById(Integer.parseInt(slug_or_id));
        } else
            thread = threadService.getThreadBySlug(slug_or_id);
        if(thread == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if(!userService.userExists(vote.getNickname())){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Thread votedThread = voteService.createThreadVote(vote, thread.getId());
        return ResponseEntity.status(HttpStatus.OK).body(votedThread);
    }

    @RequestMapping(path="/{slug_or_id}/posts", method = RequestMethod.GET)
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


        return ResponseEntity.status(HttpStatus.OK).body(postService.getByThread(thread.getId(), limit, marker, sort, desc));
    }

    private static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }
}
