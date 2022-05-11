package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "task_answers")
public class TaskAnswer {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private long taskId;
    private String text;
}
