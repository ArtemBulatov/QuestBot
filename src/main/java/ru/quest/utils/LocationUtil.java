package ru.quest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.quest.models.Location;

@Service
public class LocationUtil {

    @Value("${location.distance}")
    private int distance;

//    public static boolean compareLocations(Location location1, Location location2) {
//        double x = Math.abs(location1.getLatitude() - location2.getLatitude());
//        double y = Math.abs(location1.getLongitude() - location2.getLongitude());
//        return x < 0.003 && y < 0.003;
//    }

    public boolean compareLocations(Location l1, Location l2) {
        return distance > getDistance(l1.getLatitude(), l2.getLatitude(), l1.getLongitude(), l2.getLongitude(), 0, 0);
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */

    public double getDistance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
