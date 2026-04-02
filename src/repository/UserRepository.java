package repository;

import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final List<User> users = new ArrayList<>();

    public void save(User user) {
        if (findById(user.getUserid()).isEmpty()) {
            users.add(user);
        }
    }

    public Optional<User> findById(String userId) {
        return users.stream().filter(user -> user.getUserid().equalsIgnoreCase(userId)).findFirst();
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public void replaceAll(List<User> updatedUsers) {
        users.clear();
        users.addAll(updatedUsers);
    }
}
