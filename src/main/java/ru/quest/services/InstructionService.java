package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.Instruction;
import ru.quest.repositories.InstructionRepository;

import java.util.Optional;

@Slf4j
@Service
public class InstructionService {
    private final InstructionRepository instructionRepository;

    public InstructionService(InstructionRepository instructionRepository) {
        this.instructionRepository = instructionRepository;
    }

    public Instruction save(Instruction instruction) {
        return instructionRepository.save(instruction);
    }

    public Optional<Instruction> findByQuestId(long questId) {
        return instructionRepository.findInstructionByQuestId(questId);
    }
}
