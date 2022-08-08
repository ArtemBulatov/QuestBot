package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.Epilogue;
import ru.quest.models.Photo;
import ru.quest.repositories.EpilogueRepository;

@Service
public class EpilogueService {
    private final EpilogueRepository epilogueRepository;
    private final PhotoService photoService;

    public EpilogueService(EpilogueRepository epilogueRepository, PhotoService photoService) {
        this.epilogueRepository = epilogueRepository;
        this.photoService = photoService;
    }

    public Epilogue save(Epilogue epilogue) {
        return epilogueRepository.save(epilogue);
    }

    public Epilogue get(long id) {
        return epilogueRepository.getReferenceById(id);
    }

    public Epilogue findByQuestId(long questId) {
        return epilogueRepository.findEpilogueByQuestId(questId).orElse(null);
    }

    public void delete(Epilogue epilogue) {
        epilogueRepository.delete(epilogue);
        if (epilogue.getPhoto() != null) {
            photoService.delete(epilogue.getPhoto());
        }
    }

    public Epilogue deletePhoto(Epilogue epilogue) {
        Photo photo = epilogue.getPhoto();
        epilogue.setPhoto(null);
        epilogueRepository.save(epilogue);
        if (photo != null) {
            photoService.delete(photo);
        }
        return epilogue;
    }

    public void deleteByQuestId(long questId) {
        epilogueRepository.findEpilogueByQuestId(questId).ifPresent(this::delete);
    }
}
