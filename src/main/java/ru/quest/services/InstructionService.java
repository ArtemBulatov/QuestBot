package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.Instruction;
import ru.quest.models.Photo;
import ru.quest.models.Prologue;
import ru.quest.models.Video;
import ru.quest.repositories.InstructionRepository;

import java.util.Optional;

@Slf4j
@Service
public class InstructionService {
    private final InstructionRepository instructionRepository;
    private final VideoService videoService;

    public InstructionService(InstructionRepository instructionRepository, VideoService videoService) {
        this.instructionRepository = instructionRepository;
        this.videoService = videoService;
    }

    public Instruction save(Instruction instruction) {
        return instructionRepository.save(instruction);
    }

    public Optional<Instruction> findByQuestId(long questId) {
        return instructionRepository.findInstructionByQuestId(questId);
    }

    public Instruction get(long id) {
        return instructionRepository.getReferenceById(id);
    }

    public void delete(Instruction instruction) {
        instructionRepository.delete(instruction);
        if (instruction.getVideo() != null) {
            videoService.delete(instruction.getVideo());
        }
    }

    public Instruction deleteVideo(Instruction instruction) {
        Video video = instruction.getVideo();
        instruction.setVideo(null);
        instructionRepository.save(instruction);
        if (video != null) {
            videoService.delete(video);
        }
        return instruction;
    }
}
