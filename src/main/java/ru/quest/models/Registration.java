package ru.quest.models;

import lombok.Data;
import ru.quest.enums.RegistrationStatus;

import javax.persistence.*;

@Entity
@Data
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private long userId;
    private long questId;
    private String teamName;
    private int teamMembersNumber;
    private boolean confirmed;
    private RegistrationStatus status;

    public Registration(long userId, long questId) {
        this.userId = userId;
        this.questId = questId;
        this.confirmed = false;
    }

    public Registration() {

    }
}
