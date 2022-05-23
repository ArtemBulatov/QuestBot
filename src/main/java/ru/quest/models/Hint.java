package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "hints")
public class Hint {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String text;
    private String hintsTask;
    private int ordinalNumber;
    private long taskId;
}
