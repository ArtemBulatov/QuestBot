package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Quest;

import java.util.Optional;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {
    Optional<Quest> findQuestByName(String name);
}
