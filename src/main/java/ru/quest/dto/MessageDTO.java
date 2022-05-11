package ru.quest.dto;

import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.quest.models.Location;

import java.util.ArrayList;
import java.util.List;

@Data
public class MessageDTO {
    private int messageId = 0;
    private long chatId = 0;
    private String callbackQueryId = "";
    private String text = "";
    private Location location;
    private List<PhotoSize> photoSizeList = new ArrayList<>();
}
