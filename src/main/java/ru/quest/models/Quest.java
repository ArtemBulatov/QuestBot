package ru.quest.models;

import lombok.Data;
import ru.quest.enums.QuestType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Data
@Table(name = "quests")
public class Quest {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private QuestType type;
    private String name;
    private String description;
    private String instruction;
    private String notificationBeforeEnd;
    private LocalDateTime dateTime;

    public String getQuestButton() {
        return name + " " + dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
