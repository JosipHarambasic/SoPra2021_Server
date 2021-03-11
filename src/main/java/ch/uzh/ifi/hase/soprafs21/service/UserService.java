package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepository;
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


    public User getUser(Long id) throws NotFoundException {
        //find user by his ID
        List<User> allUsers = this.userRepository.findAll();
        User userToReturn = null;
        for (User user: allUsers){
            if (user.getId().equals(id)){
                userToReturn = user;
            }
        }
        if(userToReturn == null){
            throw new NotFoundException("User not found");
        }

        return userToReturn;
    }


    public User createUser(User newUser) {
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.OFFLINE);

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
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        User userByPassword = userRepository.findByName(userToBeCreated.getName());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByPassword != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username and the password", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
        }
        else if (userByPassword != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
        }
    }
    public User handleRequestLogin(User userToBeChecked){
        userToBeChecked.setToken(UUID.randomUUID().toString());
        List<User> allUsers = this.userRepository.findAll();

        // set all the users to offline
        for (User user: allUsers){
            user.setStatus(UserStatus.OFFLINE);
        }

        User userByUsername = userRepository.findByUsername(userToBeChecked.getUsername());

        //check if user exists or not
        if(userByUsername == null){
            throw new InvalidKeyException("login failed because user doesn't exist");
        }

        String userPassword = userByUsername.getName();
        String userUsername = userByUsername.getUsername();

        if (!userToBeChecked.getUsername().equals(userUsername) || !userToBeChecked.getName().equals(userPassword)){
            throw new InvalidKeyException("login failed because of false credentials");
        }

        //set the logged in user to online
        userByUsername.setStatus(UserStatus.ONLINE);

        //safe the changes
        User returnedUser = userRepository.save(userByUsername);
        userRepository.flush();
        return returnedUser;
    }

}
