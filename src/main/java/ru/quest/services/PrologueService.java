package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.Photo;
import ru.quest.models.Prologue;
import ru.quest.repositories.PrologueRepository;

@Service
public class PrologueService {
    private final PrologueRepository prologueRepository;
    private final PhotoService photoService;

    public PrologueService(PrologueRepository prologueRepository, PhotoService photoService) {
        this.prologueRepository = prologueRepository;
        this.photoService = photoService;
    }

    public Prologue save(Prologue prologue) {
        return prologueRepository.save(prologue);
    }

    public Prologue get(long id) {
        return prologueRepository.getReferenceById(id);
    }

    public Prologue findByQuestId(long questId) {
        return prologueRepository.findPrologueByQuestId(questId).orElse(null);
    }

    public void delete(Prologue prologue) {
        prologueRepository.delete(prologue);
        if (prologue.getPhoto() != null) {
            photoService.delete(prologue.getPhoto());
        }
    }

    public Prologue deletePhoto(Prologue prologue) {
        Photo photo = prologue.getPhoto();
        prologue.setPhoto(null);
        prologueRepository.save(prologue);
        if (photo != null) {
            photoService.delete(photo);
        }
        return prologue;
    }

}
