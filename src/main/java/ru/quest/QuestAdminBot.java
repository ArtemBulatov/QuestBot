package ru.quest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.quest.answers.*;
import ru.quest.answers.ApproveRegistrationAnswerService;
import ru.quest.answers.EditHintAnswerService;
import ru.quest.answers.EditQuestAnswerService;
import ru.quest.answers.EditTaskAnswerService;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.AdminStatus;
import ru.quest.models.User;
import ru.quest.services.UserService;
import ru.quest.utils.MessageDtoUtil;

import java.util.HashMap;
import java.util.Map;

import static ru.quest.answers.constants.AnswerConstants.*;
import static ru.quest.answers.EditHintAnswerService.*;
import static ru.quest.answers.EditQuestAnswerService.CREATE_NEW_QUEST;
import static ru.quest.answers.EditTaskAnswerService.*;
import static ru.quest.answers.constants.EpilogueConstants.ADD_NEW_EPILOGUE;
import static ru.quest.answers.constants.EpilogueConstants.SHOW_EPILOGUE;
import static ru.quest.answers.constants.InstructionConstants.ADD_NEW_INSTRUCTION;
import static ru.quest.answers.constants.InstructionConstants.SHOW_INSTRUCTION;
import static ru.quest.answers.constants.PrologueConstants.ADD_NEW_PROLOGUE;
import static ru.quest.answers.constants.PrologueConstants.SHOW_PROLOGUE;

@Slf4j
@Component
public class QuestAdminBot extends TelegramLongPollingBot {

    public static final Map<Long, AdminStatus> statusMap = new HashMap<>();

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.bot.name}")
    private String botUserName;

    @Value("${admin.bot.token}")
    private String botToken;

    private final EditQuestAnswerService editQuestAnswerService;
    private final EditTaskAnswerService editTaskAnswerService;
    private final EditHintAnswerService editHintAnswerService;
    private final EditPrologueAnswerService editPrologueAnswerService;
    private final EditEpilogueAnswerService editEpilogueAnswerService;
    private final EditInstructionAnswerService editInstructionAnswerService;
    private final ApproveRegistrationAnswerService approveRegistrationAnswerService;
    private final ConfirmAnswerService confirmAnswerService;
    private final ResultQuestGameAnswerService resultQuestGameAnswerService;
    private final UserService userService;

    public QuestAdminBot(EditQuestAnswerService editQuestAnswerService,
                         EditTaskAnswerService editTaskAnswerService,
                         EditHintAnswerService editHintAnswerService,
                         EditPrologueAnswerService editPrologueAnswerService,
                         EditEpilogueAnswerService editEpilogueAnswerService,
                         EditInstructionAnswerService editInstructionAnswerService,
                         ApproveRegistrationAnswerService approveRegistrationAnswerService,
                         ConfirmAnswerService confirmAnswerService,
                         ResultQuestGameAnswerService resultQuestGameAnswerService,
                         UserService userService) {
        this.editQuestAnswerService = editQuestAnswerService;
        this.editTaskAnswerService = editTaskAnswerService;
        this.editHintAnswerService = editHintAnswerService;
        this.editPrologueAnswerService = editPrologueAnswerService;
        this.editEpilogueAnswerService = editEpilogueAnswerService;
        this.editInstructionAnswerService = editInstructionAnswerService;
        this.approveRegistrationAnswerService = approveRegistrationAnswerService;
        this.confirmAnswerService = confirmAnswerService;
        this.resultQuestGameAnswerService = resultQuestGameAnswerService;
        this.userService = userService;
        userService.getAll().stream().filter(User::isAdmin).forEach(user -> statusMap.put(user.getId(), AdminStatus.EDIT_QUEST));
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        MessageDTO messageDTO = MessageDtoUtil.get(update);

        if (messageDTO.getText().contains("/start ")) {
            String password = messageDTO.getText().split(" ", 2)[1];
            if (password.equals(adminPassword)) {
                User user = userService.checkUser(update);
                user.setAdmin(true);
                userService.save(user);
                statusMap.put(user.getId(), AdminStatus.EDIT_QUEST);
            }
        }

        if (!statusMap.containsKey(messageDTO.getChatId())) {
            return;
        }

        if (messageDTO.getText().equals("/quests")
                || messageDTO.getText().equals(CREATE_NEW_QUEST)
                || messageDTO.getText().matches(THIS_QUEST + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_QUEST);
        }
        else if (messageDTO.getText().matches(ADD_NEW_TASK + ":\\d+")
                || messageDTO.getText().matches(QUEST_ID + ":\\d+ " + THIS_TASK + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_TASK);
        }
        else if (messageDTO.getText().matches(ADD_NEW_HINT + ":\\d+")
                || messageDTO.getText().matches(TASK_ID + ":\\d+ " + THIS_HINT + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_HINT);
        }
        else if (messageDTO.getText().matches(SHOW_PROLOGUE+ ":\\d+") || messageDTO.getText().matches(ADD_NEW_PROLOGUE+ ":\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_PROLOGUE);
        }
        else if (messageDTO.getText().matches(SHOW_EPILOGUE+ ":\\d+") || messageDTO.getText().matches(ADD_NEW_EPILOGUE+ ":\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_EPILOGUE);
        }
        else if (messageDTO.getText().matches(SHOW_INSTRUCTION+ ":\\d+") || messageDTO.getText().matches(ADD_NEW_INSTRUCTION+ ":\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.EDIT_INSTRUCTION);
        }
        else if (messageDTO.getText().equals("/registrations")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.REGISTRATIONS);
        }
        else if (messageDTO.getText().matches(CONFIRM_PHOTO + ":\\d+.+" + USER + ":\\d+")
                || messageDTO.getText().matches(NOT_CONFIRM_PHOTO + ":\\d+.+" + USER + ":\\d+")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.QUEST_ANSWER);
        }
        else if (messageDTO.getText().equals("/quests_results")) {
            statusMap.put(messageDTO.getChatId(), AdminStatus.QUEST_RESULTS);
        }

        AnswerService answerService = switch (statusMap.get(messageDTO.getChatId())) {
            case EDIT_QUEST -> editQuestAnswerService;
            case EDIT_TASK -> editTaskAnswerService;
            case EDIT_HINT -> editHintAnswerService;
            case EDIT_PROLOGUE -> editPrologueAnswerService;
            case EDIT_EPILOGUE -> editEpilogueAnswerService;
            case EDIT_INSTRUCTION -> editInstructionAnswerService;
            case REGISTRATIONS -> approveRegistrationAnswerService;
            case QUEST_ANSWER -> confirmAnswerService;
            case QUEST_RESULTS -> resultQuestGameAnswerService;
        };

        AnswerDTO dto = answerService.getAnswer(messageDTO);

        dto.getDeleteMessages().forEach(this::sendTheDeleteMessage);
        dto.getMessages().forEach(this::sendTheMessage);
        dto.getEditMessages().forEach(this::sendTheEditMessage);
        dto.getPhotoMessages().forEach(this::sendThePhoto);
        dto.getVideoMessages().forEach(this::sendTheVideo);
        dto.getEditMessageMedia().forEach(this::sendTheMedia);
        dto.getEditMessageCaptions().forEach(this::sendEditMessage);
        dto.getCallbackQueries().forEach(this::sendTheCallbackQuery);
        dto.getSendDocuments().forEach(this::sendTheDocument);
        dto.getSendLocations().forEach(this::sendTheLocation);
    }

    public void sendTheMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheEditMessage(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendEditMessage(EditMessageCaption message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendThePhoto(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheVideo(SendVideo sendVideo) {
        try {
            execute(sendVideo);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheDocument(SendDocument sendDocument) {
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheLocation(SendLocation sendLocation) {
        try {
            execute(sendLocation);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheMedia(EditMessageMedia editMessageMedia) {
        try {
            execute(editMessageMedia);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheCallbackQuery(AnswerCallbackQuery callbackQuery) {
        try {
            execute(callbackQuery);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendTheDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

}
