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
}
