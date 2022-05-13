package ru.quest.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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
    private boolean answered;
    @OneToOne
    private Location userLocation;
    @OneToMany
    private List<Hint> usedHints;
}
