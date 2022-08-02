package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}
