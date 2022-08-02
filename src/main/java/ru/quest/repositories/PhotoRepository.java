package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
}
