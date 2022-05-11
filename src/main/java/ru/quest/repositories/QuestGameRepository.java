package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.QuestGame;

import java.util.List;
import java.util.Optional;

public interface QuestGameRepository extends JpaRepository<QuestGame, Long> {
    Optional<QuestGame> findQuestGameByQuestIdAndUserIdAndIsOver(long questId, long userId, boolean isOver);
    List<QuestGame> findAllByQuestIdAndUserId(long questId, long userId);
    List<QuestGame> findAllByIsOver(boolean isOver);
}
