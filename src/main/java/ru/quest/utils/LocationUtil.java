package ru.quest.utils;

import org.springframework.stereotype.Service;
import ru.quest.models.Location;

@Service
public class LocationUtil {

    public static boolean compareLocations(Location location1, Location location2) {
        double x = Math.abs(location1.getLatitude() - location2.getLatitude());
        double y = Math.abs(location1.getLongitude() - location2.getLongitude());
        return x < 0.003 && y < 0.003;
    }
}
