package app.controllers;

import app.service.UserService;
import app.models.ServiceAnswer;
import app.models.User;
import app.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping(path = "/api/user")
public class UserController {

    final private UserService userService;

    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
    }

    @RequestMapping(path = "/{nickname}/create", method = RequestMethod.POST)
    public ResponseEntity createUser(@PathVariable(name = "nickname") String nickname,
                                     @RequestBody User user){
        user.setNickname(nickname);
        if(!user.isNotNull()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int errorCode = userService.createNewUser(user);

        if(errorCode == Status.DUPLICATE){
            ArrayList<User> duplicates = userService.getDuplicates(user);
            ResponseEntity tmp = ResponseEntity.status(HttpStatus.CONFLICT).body(duplicates);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(duplicates);
        }
        if(errorCode == Status.UNDEFINED){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


    @RequestMapping(path = "/{nickname}/profile", method = RequestMethod.GET)
    public ResponseEntity<User> getUserProfile(@PathVariable(name = "nickname") String nickname){
        User user = userService.getUserByNick(nickname);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }


    @RequestMapping(path = "/{nickname}/profile", method = RequestMethod.POST)
    public ResponseEntity changeUserData(@PathVariable(name = "nickname") String nickname,
                                     @RequestBody User newUser){
        newUser.setNickname(nickname);
        ServiceAnswer<User> serviceAnswer = userService.updateUser(newUser);

        if(serviceAnswer.getErrorCode() == Status.DUPLICATE){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if(serviceAnswer.getErrorCode() == Status.UNDEFINED){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(serviceAnswer.getData());
    }
}
