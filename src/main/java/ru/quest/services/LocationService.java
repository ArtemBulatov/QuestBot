package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.Location;
import ru.quest.repositories.LocationRepository;

@Slf4j
@Service
public class LocationService {
    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }

    public Location get(long id) {
        return locationRepository.getById(id);
    }

    public void delete(Location location) {
        locationRepository.delete(location);
    }
}
