package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.TaskCompleting;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCompletingRepository extends JpaRepository<TaskCompleting, Long> {
    Optional<TaskCompleting> findTaskCompletingByQuestGameIdAndTaskId(long questGameId, long taskId);
    List<TaskCompleting> findAllByQuestGameId(long questGameId);
}
