package ru.quest.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    private long id;
    private String userName;
    private String firstName;
    private String lastName;
    private boolean isAdmin;
}
