package ru.quest.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "task_completing")
public class TaskCompleting {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private long questGameId;
    private long taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int hintsUsedNumber;
    @OneToOne
    private Location userLocation;
}
