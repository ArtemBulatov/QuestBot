package ru.quest.answers;

import org.springframework.stereotype.Service;
import ru.quest.QuestBot;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.MessageDTO;

import static ru.quest.answers.constants.AnswerConstants.*;

@Service
public class ConfirmAnswerService implements AnswerService {
    private final QuestAnswerService questAnswerService;
    private final QuestBot questBot;

    public ConfirmAnswerService(QuestAnswerService questAnswerService, QuestBot questBot) {
        this.questAnswerService = questAnswerService;
        this.questBot = questBot;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();
        System.out.println(dto.getText());

        if (dto.getText().matches(CONFIRM_PHOTO + ":\\d+.+" + USER + ":\\d+")
                || dto.getText().matches(NOT_CONFIRM_PHOTO + ":\\d+.+" + USER + ":\\d+")) {
            questBot.sendAnswers(questAnswerService.getAnswer(dto));
            answerDTO.getMessages().add(getSendMessage("Ваш ответ отправлен участникам", dto.getChatId()));
        }
        return answerDTO;
    }
}
