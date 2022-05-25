package ru.quest.models;

import lombok.Data;
import ru.quest.enums.AnswerType;

import javax.persistence.*;

@Entity
@Data
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String text;
    private AnswerType answerType;
    private String answer;
    private String trueAnswer;
    private String falseAnswer;
    private boolean isLast;
    private long questId;
    @OneToOne
    private Location location;
    @OneToOne
    private Photo photo;
}
