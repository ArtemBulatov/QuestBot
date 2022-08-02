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
    @Column(length = 1000)
    private String text;
    @Column(length = 1000)
    private String hintsTask;
    private int ordinalNumber;
    private long taskId;
}
