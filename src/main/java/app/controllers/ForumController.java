package app.controllers;

import app.service.ForumService;
import app.service.ThreadService;
import app.service.UserService;
import app.models.ServiceAnswer;
import app.models.Forum;
import app.models.Thread;
import app.models.User;
import app.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/forum")
public class ForumController {

    final private ForumService forumService;
    final private ThreadService threadService;
    final private UserService userService;

    @Autowired
    public ForumController(ForumService forumService, UserService userService, ThreadService threadService){
        this.forumService = forumService;
        this.userService = userService;
        this.threadService = threadService;
    }

    @RequestMapping(path = "/create", method = RequestMethod.POST)
    public ResponseEntity createForum(@RequestBody Forum forum){

        User user = null;

        user = userService.getUserByNick(forum.getUser());
        if(user == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        forum.setUser(user.getNickname());

        ServiceAnswer<Forum> serviceAnswer = forumService.createNewForum(forum);

        if(serviceAnswer.getErrorCode() == Status.DUPLICATE){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceAnswer.getData());
        }
        if(serviceAnswer.getErrorCode() == Status.UNDEFINED){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAnswer.getData());
    }

    @RequestMapping(path = "/{slug}/details", method = RequestMethod.GET)
    public ResponseEntity getForumDetails(@PathVariable(name = "slug") String slug){

        Forum forum = forumService.getForumBySlug(slug);

        if(forum == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(forum);
    }

    @RequestMapping(path = "/{slug}/threads", method = RequestMethod.GET)
    public ResponseEntity getForumThreads(@PathVariable(name = "slug") String slug,
                                  @RequestParam(name = "limit", required = false) Integer limit,
                                  @RequestParam(name = "since", required = false) String since,
                                  @RequestParam(name = "desc", required = false) Boolean desc){

        Forum forum = forumService.getForumBySlug(slug);

        if(forum == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Thread> threads = threadService.getThreadsByForum(slug, limit, since, desc);
        if(threads == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(threads);
    }

    @RequestMapping(path="/{slug}/create", method = RequestMethod.POST)
    public ResponseEntity createThread(@PathVariable(name = "slug") String slug,
                                       @RequestBody Thread thread){

        User user = userService.getUserByNick(thread.getAuthor());
        Forum forum = forumService.getForumBySlug(slug);
        if(user == null || forum == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        thread.setAuthor(user.getNickname());
        thread.setForum(forum.getSlug());

        ServiceAnswer<Thread> serviceAnswer = threadService.createNewThread(thread);

        if(serviceAnswer.getErrorCode() == Status.DUPLICATE){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceAnswer.getData());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAnswer.getData());
    }


    @RequestMapping(path="/{slug}/users", method = RequestMethod.GET)
    public ResponseEntity getThreadCreators(@PathVariable(name = "slug") String slug,
                                @RequestParam(name = "limit", required = false) Integer limit,
                                @RequestParam(name = "since", required = false) String since,
                                @RequestParam(name = "desc", required = false) Boolean desc){
        if(forumService.getForumBySlug(slug) == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<User> users = userService.getUsersByForum(slug, limit, since, desc);
        if(users == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(users);
    }
}
