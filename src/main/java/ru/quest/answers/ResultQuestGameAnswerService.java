package ru.quest.answers;

import org.springframework.stereotype.Service;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;

@Service
public class ResultQuestGameAnswerService implements AnswerService {
    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        return answerDTO;
    }
}
