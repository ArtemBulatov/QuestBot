package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Epilogue;

import java.util.Optional;

@Repository
public interface EpilogueRepository extends JpaRepository<Epilogue, Long> {
    Optional<Epilogue> findEpilogueByQuestId(long questId);
}
