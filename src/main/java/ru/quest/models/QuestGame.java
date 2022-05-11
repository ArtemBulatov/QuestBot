package ru.quest.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "quest_games")
public class QuestGame {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private long userId;
    private long questId;
    private int points;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isOver;
}
