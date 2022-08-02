package ru.quest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quest.models.Registration;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findAllByUserId(long userId);
    List<Registration> findAllByQuestId(long questId);
    Optional<Registration> findRegistrationByQuestIdAndUserId(long questId, long userId);
}
