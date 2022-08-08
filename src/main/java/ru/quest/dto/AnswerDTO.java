package ru.quest.dto;

import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnswerDTO {
    private List<SendMessage> messages = new ArrayList<>();
    private List<EditMessageText> editMessages = new ArrayList<>();
    private List<EditMessageCaption> editMessageCaptions = new ArrayList<>();
    private List<EditMessageMedia> editMessageMedia = new ArrayList<>();
    private List<SendPhoto> photoMessages = new ArrayList<>();
    private List<SendDocument> sendDocuments = new ArrayList<>();
    private List<SendLocation> sendLocations = new ArrayList<>();
    private List<EditMessageReplyMarkup> editMessageReplyMarkups = new ArrayList<>();
    private List<AnswerCallbackQuery> callbackQueries = new ArrayList<>();
    private List<DeleteMessage> deleteMessages = new ArrayList<>();

    public void addAnswer(Object answer) {
        if (answer instanceof SendMessage) {
            messages.add((SendMessage) answer);
        }
        else if (answer instanceof EditMessageText) {
            editMessages.add((EditMessageText) answer);
        }
        else if (answer instanceof EditMessageCaption) {
            editMessageCaptions.add((EditMessageCaption) answer);
        }
        else if (answer instanceof EditMessageMedia) {
            editMessageMedia.add((EditMessageMedia) answer);
        }
        else if (answer instanceof SendPhoto) {
            photoMessages.add((SendPhoto) answer);
        }
        else if (answer instanceof SendDocument) {
            sendDocuments.add((SendDocument) answer);
        }
        else if (answer instanceof SendLocation) {
            sendLocations.add((SendLocation) answer);
        }
        else if (answer instanceof EditMessageReplyMarkup) {
            editMessageReplyMarkups.add((EditMessageReplyMarkup) answer);
        }
        else if (answer instanceof AnswerCallbackQuery) {
            callbackQueries.add((AnswerCallbackQuery) answer);
        }
        else if (answer instanceof DeleteMessage) {
            deleteMessages.add((DeleteMessage) answer);
        }
    }
}
