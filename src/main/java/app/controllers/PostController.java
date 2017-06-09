package app.controllers;

import app.service.ForumService;
import app.service.PostService;
import app.service.ThreadService;
import app.service.UserService;
import app.models.Post;
import app.models.PostFull;
import app.models.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


@RestController
public class PostController {

    final private ForumService forumService;
    final private ThreadService threadService;
    final private UserService userService;
    final private PostService postService;

    @Autowired
    public PostController(ForumService forumService, ThreadService threadService, UserService userService, PostService postService){
        this.forumService = forumService;
        this.threadService = threadService;
        this.userService = userService;
        this.postService = postService;
    }

    @RequestMapping(path = "/api/post/{id}/details", method = RequestMethod.GET)
    public ResponseEntity detailsPost(@PathVariable(name = "id") int id,
                                      @RequestParam(name = "related", required = false) Set<String> related){

        PostFull postFull = new PostFull();
        postFull.setPost(postService.getPostById(id));
        if(postFull.getPost() == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if(related != null){
            if(related.contains("user")){
                postFull.setAuthor(userService.getUserByNickname(postFull.getPost().getAuthor()));
                if(postFull.getAuthor() == null){
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            }
            if(related.contains("forum")){
                postFull.setForum(forumService.getBySlug(postFull.getPost().getForum()));
                if(postFull.getForum() == null){
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            }
            if(related.contains("thread")){
                postFull.setThread(threadService.getThreadById(postFull.getPost().getThread()));
                if(postFull.getThread() == null){
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(postFull);
    }

    @RequestMapping(path = "/api/post/{id}/details", method = RequestMethod.POST)
    public ResponseEntity updatePost(@PathVariable(name = "id") int id,
                                     @RequestBody PostUpdate body){

        Post post = postService.update(body, id);
        if(post == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(post);
    }
}
