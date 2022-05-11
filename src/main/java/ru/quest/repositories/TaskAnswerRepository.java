package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.TaskAnswer;

public interface TaskAnswerRepository extends JpaRepository<TaskAnswer, Long> {
}
