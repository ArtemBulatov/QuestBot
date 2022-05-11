package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
