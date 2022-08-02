package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Instruction;

import java.util.Optional;

@Repository
public interface InstructionRepository extends JpaRepository<Instruction, Long> {
    Optional<Instruction> findInstructionByQuestId(long questId);
}
