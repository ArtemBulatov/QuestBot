package ru.quest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.quest.answers.AnswerService;
import ru.quest.answers.QuestAnswerService;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.UserStatus;
import ru.quest.models.User;
import ru.quest.services.UserService;
import ru.quest.utils.MessageDtoUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class QuestBot extends TelegramLongPollingBot {
    public static final Map<Long, UserStatus> statusMap = new HashMap<>();

    @Value("${user.bot.name}")
    private String botUserName;

    @Value("${user.bot.token}")
    private String botToken;

    private final QuestAnswerService questAnswerService;
    private final UserService userService;

    public QuestBot(QuestAnswerService questAnswerService, UserService userService) {
        this.questAnswerService = questAnswerService;
        this.userService = userService;
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

        if (messageDTO.getText().equals("/start")) {
            User user = userService.find(messageDTO.getChatId());
            if (user == null) {
                user = new User();
                user.setId(messageDTO.getChatId());
            }
            user.setUserName(update.getMessage().getChat().getUserName());
            user.setFirstName(update.getMessage().getChat().getFirstName());
            user.setLastName(update.getMessage().getChat().getLastName());
            userService.save(user);
        }

        AnswerService answerService = questAnswerService;
        AnswerDTO dto = answerService.getAnswer(messageDTO);

        dto.getDeleteMessages().forEach(this::sendTheDeleteMessage);
        dto.getMessages().forEach(this::sendTheMessage);
        dto.getEditMessages().forEach(this::sendTheEditMessage);
        dto.getPhotoMessages().forEach(this::sendThePhoto);
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
            e.printStackTrace();
        }
    }

    public void sendTheEditMessage(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendEditMessage(EditMessageCaption message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendThePhoto(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTheDocument(SendDocument sendDocument) {
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTheMedia(EditMessageMedia editMessageMedia) {
        try {
            execute(editMessageMedia);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTheCallbackQuery(AnswerCallbackQuery callbackQuery) {
        try {
            execute(callbackQuery);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTheLocation(SendLocation sendLocation) {
        try {
            execute(sendLocation);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTheDeleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
