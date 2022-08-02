package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "instructions")
public class Instruction {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private long questId;

    @Column(length = 1000)
    private String text;

    @OneToOne
    private Video video;
}
