package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
