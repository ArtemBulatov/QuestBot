package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quest.models.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
}
