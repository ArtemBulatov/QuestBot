package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
}
