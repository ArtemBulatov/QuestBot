package ru.quest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Video;

@Data
@AllArgsConstructor
public class VideoDTO {
    private Video video;
}
