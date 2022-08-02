package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quest.models.User;
import ru.quest.repositories.UserRepository;

import java.util.List;

@Slf4j
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

    public User checkUser(Update update) {
        User user = find(update.getMessage().getFrom().getId());
        if (user == null) {
            user = new User();
            user.setId(update.getMessage().getFrom().getId());
        }
        user.setUserName(update.getMessage().getChat().getUserName());
        user.setFirstName(update.getMessage().getChat().getFirstName());
        user.setLastName(update.getMessage().getChat().getLastName());
        userRepository.save(user);
        return user;
    }
}
