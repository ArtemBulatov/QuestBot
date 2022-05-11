package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.User;
import ru.quest.repositories.UserRepository;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User get(long id) {
        return userRepository.getById(id);
    }

    public User find(long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }
}
