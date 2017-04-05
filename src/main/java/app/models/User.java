package app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    public User(@JsonProperty("nickname") String nickname, @JsonProperty("fullname") String fullname,
                @JsonProperty("about") String about, @JsonProperty("email") String email){
        this.nickname = nickname;
        this.fullname = fullname;
        this.about = about;
        this.email = email;
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

    public boolean isNotNull(){
        return !(nickname == null || fullname == null ||
                about == null || email == null);
    }
}
