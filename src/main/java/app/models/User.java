package app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class User {
    @JsonProperty
    private String about;
    @JsonProperty
    private String email;
    @JsonProperty
    private String fullname;
    @JsonProperty
    private String nickname;


    @JsonCreator
    public User(@JsonProperty("nickname") String nickname,
                @JsonProperty("fullname") String fullname,
                @JsonProperty("about") String about,
                @JsonProperty("email") String email){
        this.nickname = nickname;
        this.fullname = fullname;
        this.about = about;
        this.email = email;
    }

    public static List<User> parseMap(List<Map<String, Object>> objs) {
        List<User> u = new ArrayList<>();
        for(Map<String, Object>row: objs){
            u.add(new User(
                row.get("nickname").toString(), row.get("fullname").toString(),
                row.get("about").toString(), row.get("email").toString()
            ));
        }

        return u;
    }

    public String getNickname() {
        return nickname;
    }

    public String getFullname() {
        return fullname;
    }

    public String getAbout() {
        return about;
    }

    public String getEmail() {
        return email;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static boolean isIsSet(User user){
        return !(user.nickname == null || user.fullname == null ||
                user.about == null || user.email == null);
    }
}
