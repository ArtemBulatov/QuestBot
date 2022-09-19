package ru.quest.models;

import lombok.Data;
import ru.quest.enums.QuestType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.quest.answers.constants.AnswerConstants.CHANGE_INDEX;
import static ru.quest.answers.constants.AnswerConstants.THIS_QUEST;

@Entity
@Data
@Table(name = "quests")
public class Quest {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private QuestType type;
    private String name;
    @Column(length = 1000)
    private String description;
    @Column(length = 1000)
    private String instruction;
    @Column(length = 1000)
    private String notificationBeforeEnd;
    private LocalDateTime dateTime;
    private boolean deleted;

    public String getQuestButton() {
        return name + " " + dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public static String getButtonDataToShowQuest(long questId, int index) {
        return THIS_QUEST + ":" + questId + " " + CHANGE_INDEX + ":" + index;
    }
}
