package ru.quest.answers;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.models.Photo;
import ru.quest.utils.ButtonsUtil;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface AnswerService {

    AnswerDTO getAnswer(MessageDTO messageDTO);

    default SendMessage getSendMessage(String message, String[] buttons, boolean markDown, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.enableMarkdownV2(markDown);
        sendMessage.setText(message);
        if (buttons != null && buttons.length > 0) {
            sendMessage.setReplyMarkup(ButtonsUtil.getReplyButtons(buttons, true));
        }
        return sendMessage;
    }

    default SendMessage getSendMessageWithInlineButtons(String message, List<InlineButtonDTO> buttons, int countInLine, boolean markDown, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.enableMarkdownV2(markDown);
        sendMessage.setText(message);
        if (buttons != null && buttons.size() > 0) {
            sendMessage.setReplyMarkup(ButtonsUtil.getInlineButtons(buttons, countInLine));
        }
        return sendMessage;
    }

    default SendMessage getSendMessage(String message, String[] buttons, long chatId) {
        return getSendMessage(message, buttons, false, chatId);
    }
    default SendMessage getSendMessage(String message, long chatId) {
        return getSendMessage(message, null, false, chatId);
    }

    default SendMessage getSendMessage(String message, boolean markDown, ReplyKeyboard replyKeyboard, long chatId) {
        SendMessage sendMessage = getSendMessage(message, null, markDown, chatId);
        sendMessage.setReplyMarkup(replyKeyboard);
        return sendMessage;
    }

    default EditMessageText getEditMessageText(String message, InlineKeyboardMarkup markup, boolean markDown, long chatId, int messageId) {
        EditMessageText editMessageText = getEditMessageText(message, null, 0, markDown, chatId, messageId);
        editMessageText.setReplyMarkup(markup);
        return editMessageText;
    }

    default EditMessageText getEditMessageText(String message, List<InlineButtonDTO> buttons, int countInLine, boolean markDown, long chatId, int messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        if (markDown) editMessageText.setParseMode("MarkDownV2");
        editMessageText.setText(message);
        if (buttons != null && buttons.size() > 0) {
            editMessageText.setReplyMarkup(ButtonsUtil.getInlineButtons(buttons, countInLine));
        }
        return editMessageText;
    }

    default DeleteMessage getDeleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));
        deleteMessage.setMessageId(messageId);
        return deleteMessage;
    }

    default AnswerCallbackQuery getAnswerCallbackQuery(String message, int cacheTime, String callbackId) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackId);
        answerCallbackQuery.setText(message);
        answerCallbackQuery.setCacheTime(cacheTime);
        return answerCallbackQuery;
    }

    default SendPhoto getSendPhoto(String message, Photo photo, boolean markDown, ReplyKeyboard replyKeyboard, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photo.getBytes()), photo.getName()));
        sendPhoto.setCaption(message);
        if (markDown) sendPhoto.setParseMode("MarkDownV2");
        sendPhoto.setReplyMarkup(replyKeyboard);

        return sendPhoto;
    }

}
