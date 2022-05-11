package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.Hint;

import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {
    List<Hint> findAllByTaskId(long id);
    void deleteAllByTaskId(long id);
}
