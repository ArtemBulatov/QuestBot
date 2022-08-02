package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.Hint;
import ru.quest.repositories.HintRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class HintService {
    private final HintRepository hintRepository;

    public HintService(HintRepository hintRepository) {
        this.hintRepository = hintRepository;
    }

    public Hint save(Hint hint) {
        return hintRepository.save(hint);
    }

    public Hint get(long id) {
        return hintRepository.getById(id);
    }

    public List<Hint> getAllByTaskId(long id) {
        List<Hint> hints = hintRepository.findAllByTaskId(id);
        hints.sort(Comparator.comparing(Hint::getOrdinalNumber));
        return hints;
    }

    public void delete(Hint hint) {
        try {
            hintRepository.delete(hint);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void deleteAllByTaskId(long id) {
        hintRepository.findAllByTaskId(id).forEach(this::delete);
    }
}
