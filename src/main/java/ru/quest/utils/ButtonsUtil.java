package ru.quest.utils;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.quest.dto.InlineButtonDTO;

import java.util.ArrayList;
import java.util.List;

@Service
public class ButtonsUtil {

    public static ReplyKeyboardMarkup getReplyButtons(String[] buttonsText, boolean isOneTime) {

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(isOneTime);
        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();

        if (buttonsText.length == 2) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton(buttonsText[0]));
            keyboardRow.add(new KeyboardButton(buttonsText[1]));
            keyboard.add(keyboardRow);
        }
        else {
            for (String buttonText : buttonsText){
                KeyboardRow keyboardNextRow = new KeyboardRow();
                keyboardNextRow.add(new KeyboardButton(buttonText));
                keyboard.add(keyboardNextRow);
            }
        }
        keyboard.add(0, keyboardFirstRow);

        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getInlineButtons(List<InlineButtonDTO> buttonDTOList, int countInLine) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getInlineButtonsRowList(buttonDTOList, countInLine));
        return inlineKeyboardMarkup;
    }

    public static List<List<InlineKeyboardButton>> getInlineButtonsRowList(List<InlineButtonDTO> buttonDTOList, int countInLine) {
        List<List<InlineKeyboardButton>> rowList= new ArrayList<>();

        for (int i = 0; i < buttonDTOList.size(); i = i+countInLine) {
            List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
            rowList.add(keyboardButtonsRow);

            for (int x = i; x < i + countInLine && x < buttonDTOList.size(); x++) {
                InlineButtonDTO buttonDTO = buttonDTOList.get(x);
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonDTO.getName());
                button.setCallbackData(buttonDTO.getValue());
                keyboardButtonsRow.add(button);
            }
        }
        return rowList;
    }

    public static ReplyKeyboardMarkup getLocationButton(String buttonText) {
        KeyboardButton button = new KeyboardButton(buttonText);
        button.setRequestLocation(true);
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(button);
        List<KeyboardRow> rowList = new ArrayList<>();
        rowList.add(keyboardRow);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(rowList);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }
}
