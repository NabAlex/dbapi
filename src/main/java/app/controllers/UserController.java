package app.controllers;

import app.service.UserInForumService;
import app.service.UserService;
import app.util.ResultPack;
import app.models.User;
import app.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired private UserService userService;
    
    @RequestMapping(path = "/api/user/{nickname}/create", method = RequestMethod.POST)
    public ResponseEntity createUser(@PathVariable(name = "nickname") String nickname,
                                     @RequestBody User user){
        user.setNickname(nickname);

        if(!User.isIsSet(user))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        int errorCode = userService.createNewUser(user);

        if(errorCode == Status.CONFLICT){
            List<User> duplicates = userService.getDuplicates(user, null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(duplicates);
        } else if(errorCode == Status.NOTFOUND){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


    @RequestMapping(path = "/api/user/{nickname}/profile", method = RequestMethod.GET)
    public ResponseEntity<User> getUserProfile(@PathVariable(name = "nickname") String nickname){
        User user = userService.getUserByNickname(nickname);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }


    @RequestMapping(path = "/api/user/{nickname}/profile", method = RequestMethod.POST)
    public ResponseEntity changeUserData(@PathVariable(name = "nickname") String nickname,
                                     @RequestBody User newUser){
        newUser.setNickname(nickname);
        ResultPack<User> serviceAnswer = userService.updateUser(newUser);

        if(serviceAnswer.getErrorCode() == Status.CONFLICT){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if(serviceAnswer.getErrorCode() == Status.NOTFOUND){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(serviceAnswer.getData());
    }
}
