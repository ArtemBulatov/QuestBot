package ru.quest.models;

import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.answers.EditQuestAnswerService;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.utils.ButtonsUtil;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static ru.quest.answers.AnswerConstants.*;
import static ru.quest.answers.PrologueConstants.*;

@Entity
@Data
@Table(name = "prologues")
public class Prologue implements BotsVisualization{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true)
    private long questId;

    @Column(length = 10000)
    private String text;

    @OneToOne
    private Photo photo;

    @Override
    public Object getMessage(long chatId) {
        if (photo == null) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(getTextMessage());
            sendMessage.setReplyMarkup(getInlineKeyboardMarkup());
            return sendMessage;
        }
        else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(chatId));
            sendPhoto.setCaption(getTextMessage());
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photo.getBytes()), photo.getName()));
            sendPhoto.setReplyMarkup(getInlineKeyboardMarkup());
            return sendPhoto;
        }
    }

    @Override
    public String getTextMessage() {
        return PROLOGUE + ": \n\"" + text + "\"";
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        if (photo == null) {
            buttonDTOList.add(new InlineButtonDTO(ADD_PROLOGUE_PHOTO, ADD_PROLOGUE_PHOTO + ":" + id));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(DELETE_PROLOGUE_PHOTO, DELETE_PROLOGUE_PHOTO + ":" + id));
        }
        buttonDTOList.add(new InlineButtonDTO(CHANGE_PROLOGUE_TEXT, CHANGE_PROLOGUE_TEXT+ ":" + id));
        buttonDTOList.add(new InlineButtonDTO(DELETE_PROLOGUE, DELETE_PROLOGUE+ ":" + id));
        buttonDTOList.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(questId, 0)));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }
}
