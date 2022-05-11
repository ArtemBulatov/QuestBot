package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.QuestGame;
import ru.quest.repositories.QuestGameRepository;
import ru.quest.utils.KhantyMansiyskDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestGameService {
    private final QuestGameRepository questGameRepository;

    public QuestGameService(QuestGameRepository questGameRepository) {
        this.questGameRepository = questGameRepository;
    }

    public QuestGame save(QuestGame questGame) {
        return questGameRepository.save(questGame);
    }

    public QuestGame create(long questId, long userId) {
        List<QuestGame> questGames = questGameRepository.findAllByQuestIdAndUserId(questId, userId);
        questGames.forEach(questGame -> {
            if (!questGame.isOver()) {
                questGame.setOver(true);
                questGameRepository.save(questGame);
            }
        });
        QuestGame questGame = new QuestGame();
        questGame.setQuestId(questId);
        questGame.setUserId(userId);
        questGame.setStartTime(KhantyMansiyskDateTime.now());
        return questGameRepository.save(questGame);
    }

    public QuestGame get(long questId, long userId) {
        return questGameRepository.findQuestGameByQuestIdAndUserIdAndIsOver(questId, userId, false).orElse(null);
    }

    public List<QuestGame> getActiveQuestGames() {
        LocalDateTime dateTime = KhantyMansiyskDateTime.now();
        return questGameRepository.findAllByIsOver(false)
                .stream().filter(questGame -> questGame.getStartTime().toLocalDate().isEqual(dateTime.toLocalDate()))
                .collect(Collectors.toList());
    }
}
