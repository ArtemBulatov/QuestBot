package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Prologue;

import java.util.Optional;

@Repository
public interface PrologueRepository extends JpaRepository<Prologue, Long> {
    Optional<Prologue> findPrologueByQuestId(long questId);
}
