package ru.quest.models;

import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.utils.ButtonsUtil;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static ru.quest.answers.constants.AnswerConstants.BACK;
import static ru.quest.answers.constants.InstructionConstants.*;
import static ru.quest.answers.constants.PrologueConstants.*;
import static ru.quest.answers.constants.PrologueConstants.DELETE_PROLOGUE;

@Entity
@Data
@Table(name = "instructions")
public class Instruction implements BotsVisualization{
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private long questId;

    @Column(length = 1000)
    private String text;

    @OneToOne
    private Video video;

    @Override
    public Object getMessage(long chatId) {
        if (video == null) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(getTextMessage());
            sendMessage.setReplyMarkup(getInlineKeyboardMarkup());
            return sendMessage;
        }
        else {
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(String.valueOf(chatId));
            sendVideo.setCaption(getTextMessage());
            sendVideo.setVideo(new InputFile(new ByteArrayInputStream(video.getBytes()), video.getName()));
            sendVideo.setReplyMarkup(getInlineKeyboardMarkup());
            return sendVideo;
        }
    }

    @Override
    public String getTextMessage() {
        return INSTRUCTION + ": \n\"" + text + "\"";
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        if (video == null) {
            buttonDTOList.add(new InlineButtonDTO(ADD_INSTRUCTION_VIDEO, ADD_INSTRUCTION_VIDEO + ":" + id));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(DELETE_INSTRUCTION_VIDEO, DELETE_INSTRUCTION_VIDEO + ":" + id));
        }
        buttonDTOList.add(new InlineButtonDTO(CHANGE_INSTRUCTION_TEXT, CHANGE_INSTRUCTION_TEXT+ ":" + id));
        buttonDTOList.add(new InlineButtonDTO(DELETE_INSTRUCTION, DELETE_INSTRUCTION+ ":" + id));
        buttonDTOList.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(questId, 0)));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }
}
