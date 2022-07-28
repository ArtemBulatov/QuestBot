package ru.quest.services;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.quest.models.Quest;
import ru.quest.repositories.QuestRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestService {
    private final QuestRepository questRepository;
    private final TaskService taskService;

    public QuestService(QuestRepository questRepository, @Lazy TaskService taskService) {
        this.questRepository = questRepository;
        this.taskService = taskService;
    }

    public Quest save(Quest quest) {
        return questRepository.save(quest);
    }

    public Quest get(long id) {
        return questRepository.getById(id);
    }

    public Quest get(String name) {
        return questRepository.findQuestByName(name).orElse(null);
    }

    public List<Quest> getaAll() {
        return questRepository.findAll();
    }

    public List<Quest> getaAllNotDeleted() {
        return questRepository.findAll().stream().filter(quest -> !quest.isDeleted()).collect(Collectors.toList());
    }

    public void delete(Quest quest) {
        quest.setDeleted(true);
        questRepository.save(quest);
        taskService.deleteAllByQuestId(quest.getId());
    }
}
