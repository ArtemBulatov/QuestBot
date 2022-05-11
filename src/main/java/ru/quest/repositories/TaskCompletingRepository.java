package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.TaskCompleting;

import java.util.List;
import java.util.Optional;

public interface TaskCompletingRepository extends JpaRepository<TaskCompleting, Long> {
    Optional<TaskCompleting> findTaskCompletingByQuestGameIdAndTaskId(long questGameId, long taskId);
    List<TaskCompleting> findAllByQuestGameId(long questGameId);
}
