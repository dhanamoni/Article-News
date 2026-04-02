package service;

import model.User;
import repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerDefaultUser() {
        userRepository.save(new User("student", "U1", "student@example.com", "password123"));
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}
