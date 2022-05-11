package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "photos")
public class Photo {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String name;

    @Column(columnDefinition = "LONGBLOB")
    @Lob
    private byte[] bytes;
}
