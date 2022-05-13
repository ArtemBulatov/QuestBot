package ru.quest.answers;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.utils.ButtonsUtil;

import java.util.List;

public interface AnswerService {

    AnswerDTO getAnswer(MessageDTO messageDTO);

    default SendMessage getSendMessage(String message, String[] buttons, boolean markDown, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.enableMarkdown(markDown);
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

    default EditMessageText getEditMessageText(String message, List<InlineButtonDTO> buttons, int countInLine, boolean markDown, long chatId, int messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        editMessageText.enableMarkdown(markDown);
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

}
