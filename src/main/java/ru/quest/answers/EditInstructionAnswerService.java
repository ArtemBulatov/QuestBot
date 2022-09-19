package ru.quest.answers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.models.Quest;
import ru.quest.models.Video;
import ru.quest.models.Instruction;
import ru.quest.services.InstructionService;
import ru.quest.services.LastAnswerService;
import ru.quest.services.VideoService;

import static ru.quest.answers.constants.InstructionConstants.*;

@Slf4j
@Service
public class EditInstructionAnswerService implements AnswerService{
    private final InstructionService instructionService;
    private final VideoService videoService;
    private final LastAnswerService lastAnswerService;
    private final EditQuestAnswerService editQuestAnswerService;

    @Value("${admin.bot.token}")
    private String botToken;

    public EditInstructionAnswerService(InstructionService instructionService, VideoService videoService, LastAnswerService lastAnswerService, EditQuestAnswerService editQuestAnswerService) {
        this.instructionService = instructionService;
        this.videoService = videoService;
        this.lastAnswerService = lastAnswerService;
        this.editQuestAnswerService = editQuestAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if (dto.getText().matches(SHOW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Instruction instruction = instructionService.findByQuestId(questId).get();
            Object answer = instruction.getMessage(dto.getChatId());
            answerDTO.addAnswer(answer);
        }
        else if (dto.getText().matches(ADD_NEW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Instruction instruction = new Instruction();
            instruction.setQuestId(questId);
            instruction.setText("");
            instruction = instructionService.save(instruction);
            answerDTO.getMessages().add(getSendMessage(ENTER_INSTRUCTION_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_INSTRUCTION_TEXT + ":" + instruction.getId(), dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_INSTRUCTION + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Instruction instruction = instructionService.get(id);
            dto.setText(Quest.getButtonDataToShowQuest(instruction.getQuestId(), 0));
            instructionService.delete(instruction);
            answerDTO = editQuestAnswerService.getAnswer(dto);
        }
        else if (dto.getText().matches(CHANGE_INSTRUCTION_TEXT + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(ENTER_INSTRUCTION_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_INSTRUCTION_TEXT + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(ADD_INSTRUCTION_VIDEO  + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(SEND_INSTRUCTION_VIDEO, dto.getChatId()));
            lastAnswerService.addLastAnswer(SEND_INSTRUCTION_VIDEO + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_INSTRUCTION_VIDEO + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Instruction instruction = instructionService.get(id);
            instruction = instructionService.deleteVideo(instruction);
            answerDTO.addAnswer(instruction.getMessage(dto.getChatId()));
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ENTER_INSTRUCTION_TEXT + ":\\d+")) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Instruction instruction = instructionService.get(id);
            if (instruction != null) {
                instruction.setText(dto.getText());
                instructionService.save(instruction);
                answerDTO.addAnswer(instruction.getMessage(dto.getChatId()));
            }
            else {
                getSendMessage("Непонятно, к какому квесту необходимо добавить текст инструкции", dto.getChatId());
            }
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(SEND_INSTRUCTION_VIDEO + ":\\d+")
                && dto.getVideoDTO() != null) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Video video = videoService.getSavedVideoFromDto(dto.getVideoDTO(), botToken);

            log.info("Видео сохранено " + video.getName() + " " + video.getBytes().length);

            Instruction instruction = instructionService.get(id);
            instruction.setVideo(video);
            instructionService.save(instruction);
            answerDTO.addAnswer(instruction.getMessage(dto.getChatId()));
        }

        return answerDTO;
    }
}
