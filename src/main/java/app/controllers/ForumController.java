package app.controllers;

import app.service.ForumService;
import app.service.ThreadService;
import app.service.UserInForumService;
import app.service.UserService;
import app.util.ResultPack;
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
public class ForumController {
    @Autowired
    private ForumService forumService;
    @Autowired
    private ThreadService threadService;
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserInForumService userInForumService;
    
    @RequestMapping(path = "/api/forum/create", method = RequestMethod.POST)
    public ResponseEntity createForum(@RequestBody Forum forum) {
        User user = userService.getUserByNickname(forum.getUser());
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        
        forum.setNick(user.getNickname());
        ResultPack<Forum> serviceAnswer = forumService.create(forum);
        
        if (serviceAnswer.getErrorCode() == Status.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceAnswer.getData());
        }
        if (serviceAnswer.getErrorCode() == Status.NOTFOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAnswer.getData());
    }
    
    @RequestMapping(path = "/api/forum/{slug}/details", method = RequestMethod.GET)
    public ResponseEntity getForumDetails(@PathVariable(name = "slug") String slug) {
        Forum forum = forumService.getBySlug(slug);
        
        if (forum == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        
        return ResponseEntity.status(HttpStatus.OK).body(forum);
    }
    
    @RequestMapping(path = "/api/forum/{slug}/threads", method = RequestMethod.GET)
    public ResponseEntity getForumThreads(@PathVariable(name = "slug") String slug,
                                          @RequestParam(name = "limit", required = false) Integer limit,
                                          @RequestParam(name = "since", required = false) String since,
                                          @RequestParam(name = "desc", required = false) Boolean desc) {
        
        Forum forum = forumService.getBySlug(slug);
        
        if (forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        List<Thread> threads = threadService.getThreadsByForum(slug, limit, since, desc);
        if (threads == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        return ResponseEntity.status(HttpStatus.OK).body(threads);
    }
    
    @RequestMapping(path = "/api/forum/{slug}/create", method = RequestMethod.POST)
    public ResponseEntity createThread(@PathVariable(name = "slug") String slug,
                                       @RequestBody Thread thread) {
        
        User user = userService.getUserByNickname(thread.getAuthor());
        Forum forum = forumService.getBySlug(slug);
        if (user == null || forum == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        thread.setAuthor(user.getNickname());
        thread.setForum(forum.getSlug());
        
        ResultPack<Thread> serviceAnswer = threadService.createNewThread(thread);
        
        if (serviceAnswer.getErrorCode() == Status.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(serviceAnswer.getData());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAnswer.getData());
    }
    
    
    @RequestMapping(path = "/api/forum/{slug}/users", method = RequestMethod.GET)
    public ResponseEntity getThreadCreators(@PathVariable(name = "slug") String slugForum,
                                            @RequestParam(name = "limit", required = false) Integer limit,
                                            @RequestParam(name = "since", required = false) String since,
                                            @RequestParam(name = "desc", required = false) Boolean desc) {
        if (forumService.getBySlug(slugForum) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        List<User> users = userInForumService.selectUsersBySlugWithFilter(slugForum, since, limit, desc);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }
}
