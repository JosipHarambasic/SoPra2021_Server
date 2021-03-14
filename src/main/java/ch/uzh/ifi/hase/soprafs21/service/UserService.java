package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserPostDTO;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.management.openmbean.InvalidKeyException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }


    public User getUser(Long id){
        //find user by his ID
        List<User> allUsers = this.userRepository.findAll();
        User userToReturn = null;
        for (User user: allUsers){
            if (user.getId().equals(id)){
                userToReturn = user;
            }
        }
        if(userToReturn == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User not found");
        }

        return userToReturn;
    }


    public User createUser(User newUser) {
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.ONLINE);
        newUser.setCreationDate(getDate());

        checkIfUserExists(newUser);

        // saves the given entity but data is only persisted in the database once flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the username and the name
     * defined in the User entity. The method will do nothing if the input is unique and throw an error otherwise.
     *
     * @param userToBeCreated
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        User userByPassword = userRepository.findByName(userToBeCreated.getName());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByPassword != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username and the password", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
        }
    }
    public User handleRequestLogin(User userToBeChecked) {
        userToBeChecked.setToken(UUID.randomUUID().toString());
        List<User> allUsers = this.userRepository.findAll();

        // set all the users to offline but I don't now how to handle more users at the same time
        for (User user: allUsers){
            user.setStatus(UserStatus.OFFLINE);
        }

        User userByUsername = userRepository.findByUsername(userToBeChecked.getUsername());

        //check if user exists or not
        if(userByUsername == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found");
        }

        String userPassword = userByUsername.getName();
        String userUsername = userByUsername.getUsername();

        //check if username and password are correct
        if (!userToBeChecked.getUsername().equals(userUsername) || !userToBeChecked.getName().equals(userPassword)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"your credentials are not correct");
        }

        //set the logged in user to online
        userByUsername.setStatus(UserStatus.ONLINE);

        //safe the changes
        User returnedUser = userRepository.save(userByUsername);
        userRepository.flush();
        return returnedUser;
    }

    public String getDate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    public void userUpdate(Long userId, UserPostDTO userEditDTO){
        List<User> users = this.userRepository.findAll();

        User updatedUser = null;

        for (User user: users){
            if (userId.equals(user.getId())){
                updatedUser = user;
            }
        }

        if (updatedUser == null){
            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
        }

        if (userEditDTO.getBirthday() != null){
            updatedUser.setBirthday(userEditDTO.getBirthday());}

        if (userEditDTO.getUsername() != null){
            updatedUser.setUsername(userEditDTO.getUsername());}

        userRepository.save(updatedUser);
        userRepository.flush();
    }

}
