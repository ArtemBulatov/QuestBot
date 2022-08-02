package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
