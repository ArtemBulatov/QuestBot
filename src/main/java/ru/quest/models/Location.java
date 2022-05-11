package ru.quest.models;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "locations")
public class Location {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private Double longitude;
    private Double latitude;

    public Location() {

    }

    public Location(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
