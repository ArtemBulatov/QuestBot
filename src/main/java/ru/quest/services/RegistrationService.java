package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.Registration;
import ru.quest.repositories.RegistrationRepository;

import java.util.List;

@Slf4j
@Service
public class RegistrationService {
    private final RegistrationRepository registrationRepository;

    public RegistrationService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public Registration save(Registration registration) {
        return registrationRepository.save(registration);
    }

    public List<Registration> getAllByUserId(long userId) {
        return registrationRepository.findAllByUserId(userId);
    }

    public List<Registration> getAllByQuestId(long questId) {
        return registrationRepository.findAllByQuestId(questId);
    }

    public Registration get(long registrationId) {
        return registrationRepository.getById(registrationId);
    }

    public Registration get(long questId, long userId) {
        return registrationRepository.findRegistrationByQuestIdAndUserId(questId, userId).orElse(null);
    }

    public void delete(long questId, long userId) {
        registrationRepository.findRegistrationByQuestIdAndUserId(questId, userId).ifPresent(registrationRepository::delete);
    }

    public void delete(long id) {
        registrationRepository.deleteById(id);
    }
}
