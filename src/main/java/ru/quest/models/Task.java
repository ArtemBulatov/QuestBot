package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String text;
    private String answer;
    private boolean isLast;
    private long questId;
    @OneToOne
    private Location location;
}
