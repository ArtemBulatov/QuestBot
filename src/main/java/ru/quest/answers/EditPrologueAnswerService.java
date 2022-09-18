package ru.quest.answers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.models.Photo;
import ru.quest.models.Prologue;
import ru.quest.models.Quest;
import ru.quest.services.LastAnswerService;
import ru.quest.services.PhotoService;
import ru.quest.services.PrologueService;

import static ru.quest.answers.PrologueConstants.*;

@Service
public class EditPrologueAnswerService implements AnswerService{

    private final PrologueService prologueService;
    private final PhotoService photoService;
    private final LastAnswerService lastAnswerService;
    private final EditQuestAnswerService editQuestAnswerService;

    @Value("${admin.bot.token}")
    private String botToken;

    public EditPrologueAnswerService(PrologueService prologueService, PhotoService photoService, LastAnswerService lastAnswerService, EditQuestAnswerService editQuestAnswerService) {
        this.prologueService = prologueService;
        this.photoService = photoService;
        this.lastAnswerService = lastAnswerService;
        this.editQuestAnswerService = editQuestAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if (dto.getText().matches(SHOW_PROLOGUE + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Prologue prologue = prologueService.findByQuestId(questId);
            Object answer = prologue.getMessage(dto.getChatId());
            answerDTO.addAnswer(answer);
        }
        else if (dto.getText().matches(ADD_NEW_PROLOGUE + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Prologue prologue = new Prologue();
            prologue.setQuestId(questId);
            prologue.setText("");
            prologue = prologueService.save(prologue);
            answerDTO.getMessages().add(getSendMessage(ENTER_PROLOGUE_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_PROLOGUE_TEXT + ":" + prologue.getId(), dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_PROLOGUE + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Prologue prologue = prologueService.get(id);
            dto.setText(Quest.getButtonDataToShowQuest(prologue.getQuestId(), 0));
            prologueService.delete(prologue);
            answerDTO = editQuestAnswerService.getAnswer(dto);
        }
        else if (dto.getText().matches(CHANGE_PROLOGUE_TEXT + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(ENTER_PROLOGUE_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_PROLOGUE_TEXT + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(ADD_PROLOGUE_PHOTO  + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            answerDTO.addAnswer(getSendMessage(SEND_PROLOGUE_PHOTO, dto.getChatId()));
            lastAnswerService.addLastAnswer(SEND_PROLOGUE_PHOTO + ":" + id, dto.getChatId());
        }
        else if (dto.getText().matches(DELETE_PROLOGUE_PHOTO + ":\\d+")) {
            long id = Long.parseLong(dto.getText().split(":", 2)[1]);
            Prologue prologue = prologueService.get(id);
            prologue = prologueService.deletePhoto(prologue);
            answerDTO.addAnswer(prologue.getMessage(dto.getChatId()));
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ENTER_PROLOGUE_TEXT + ":\\d+")) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Prologue prologue = prologueService.get(id);
            if (prologue != null) {
                prologue.setText(dto.getText());
                prologueService.save(prologue);
                answerDTO.addAnswer(prologue.getMessage(dto.getChatId()));
            }
            else {
                getSendMessage("Непонятно, к какому квесту необходимо добавить текст пролога", dto.getChatId());
            }
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(SEND_PROLOGUE_PHOTO + ":\\d+")
                && !dto.getPhotoSizeList().isEmpty()) {
            long id = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Photo photo = photoService.getSavedPhotoFromDto(dto.getPhotoSizeList(), botToken);
            Prologue prologue = prologueService.get(id);
            prologue.setPhoto(photo);
            prologueService.save(prologue);
            answerDTO.addAnswer(prologue.getMessage(dto.getChatId()));
        }

        return answerDTO;
    }

}
