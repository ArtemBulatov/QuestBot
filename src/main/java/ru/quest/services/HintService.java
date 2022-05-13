package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.Hint;
import ru.quest.repositories.HintRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        hintRepository.delete(hint);
    }

    public void deleteAllByTaskId(long id) {
        hintRepository.deleteAllByTaskId(id);
    }
}
