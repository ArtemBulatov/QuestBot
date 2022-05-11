package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.Quest;

public interface QuestRepository extends JpaRepository<Quest, Long> {
}
