package ch.uzh.ifi.hase.soprafs21.rest.dto;

import java.util.Calendar;
import java.util.Date;

public class UserPostDTO {

    private String name;

    private String username;

    private String birthday;


    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
