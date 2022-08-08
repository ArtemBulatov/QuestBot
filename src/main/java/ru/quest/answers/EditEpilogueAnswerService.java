package ru.quest.answers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.models.Epilogue;
import ru.quest.models.Photo;
import ru.quest.models.Quest;
import ru.quest.services.EpilogueService;
import ru.quest.services.LastAnswerService;
import ru.quest.services.PhotoService;

import static ru.quest.answers.EpilogueConstants.*;

@Service
public class EditEpilogueAnswerService implements AnswerService{

    private final EpilogueService epilogueService;
    private final PhotoService photoService;
    private final LastAnswerService lastAnswerService;
    private final EditQuestAnswerService editQuestAnswerService;

    @Value("${admin.bot.token}")
    private String botToken;

    public EditEpilogueAnswerService(EpilogueService epilogueService, PhotoService photoService, LastAnswerService lastAnswerService, EditQuestAnswerService editQuestAnswerService) {
        this.epilogueService = epilogueService;
        this.photoService = photoService;
        this.lastAnswerService = lastAnswerService;
        this.editQuestAnswerService = editQuestAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if (dto.getText().matches(SHOW_EPILOGUE + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Epilogue epilogue = epilogueService.findByQuestId(questId);
            Object answer = epilogue.getMessage(dto.getChatId());
            answerDTO.addAnswer(answer);
        }
        else if (dto.getText().matches(ADD_NEW_EPILOGUE + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Epilogue epilogue = new Epilogue();
            epilogue.setQuestId(questId);
            epilogue.setText("");
            epilogue = epilogueService.save(epilogue);
            answerDTO.getMessages().add(getSendMessage(ENTER_EPILOGUE_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_EPILOGUE_TEXT + ":" + epilogue.getId(), dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_EPILOGUE + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Epilogue epilogue = epilogueService.get(id);
            dto.setText(Quest.getButtonDataToShowQuest(epilogue.getQuestId(), 0));
            epilogueService.delete(epilogue);
            answerDTO = editQuestAnswerService.getAnswer(dto);
        }
        else if (dto.getText().matches(CHANGE_EPILOGUE_TEXT + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(ENTER_EPILOGUE_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_EPILOGUE_TEXT + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(ADD_EPILOGUE_PHOTO  + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(SEND_EPILOGUE_PHOTO, dto.getChatId()));
            lastAnswerService.addLastAnswer(SEND_EPILOGUE_PHOTO + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_EPILOGUE_PHOTO + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Epilogue epilogue = epilogueService.get(id);
            epilogue = epilogueService.deletePhoto(epilogue);
            answerDTO.addAnswer(epilogue.getMessage(dto.getChatId()));
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ENTER_EPILOGUE_TEXT + ":\\d+")) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Epilogue epilogue = epilogueService.get(id);
            if (epilogue != null) {
                epilogue.setText(dto.getText());
                epilogueService.save(epilogue);
                answerDTO.addAnswer(epilogue.getMessage(dto.getChatId()));
            }
            else {
                getSendMessage("Непонятно, к какому квесту необходимо добавить текст эпилога", dto.getChatId());
            }
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(SEND_EPILOGUE_PHOTO + ":\\d+")
                && !dto.getPhotoSizeList().isEmpty()) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Photo photo = photoService.getSavedPhotoFromDto(dto.getPhotoSizeList(), botToken);
            Epilogue epilogue = epilogueService.get(id);
            epilogue.setPhoto(photo);
            epilogueService.save(epilogue);
            answerDTO.addAnswer(epilogue.getMessage(dto.getChatId()));
        }

        return answerDTO;
    }
}
