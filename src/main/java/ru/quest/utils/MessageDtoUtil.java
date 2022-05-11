package ru.quest.utils;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quest.dto.MessageDTO;
import ru.quest.models.Location;

@Service
public class MessageDtoUtil {

    public static MessageDTO get(Update update) {
        MessageDTO messageDTO = new MessageDTO();

        if (update.hasMessage()) {
            messageDTO.setChatId(update.getMessage().getChatId());
            if (update.getMessage().getPhoto() != null) {
                messageDTO.setPhotoSizeList(update.getMessage().getPhoto());
            }
            if (update.getMessage().getText() != null) {
                messageDTO.setText(update.getMessage().getText());
            }
            if (update.getMessage().getLocation() != null) {

                messageDTO.setLocation(new Location(update.getMessage().getLocation().getLongitude(), update.getMessage().getLocation().getLatitude()));
            }
        }
        else if (update.hasCallbackQuery()) {
            messageDTO.setChatId(update.getCallbackQuery().getFrom().getId());
            messageDTO.setCallbackQueryId(update.getCallbackQuery().getId());
            messageDTO.setText(update.getCallbackQuery().getData());
            messageDTO.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        }
        return messageDTO;
    }
}
