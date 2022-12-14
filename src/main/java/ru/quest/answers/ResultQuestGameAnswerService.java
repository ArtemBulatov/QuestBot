package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.QuestType;
import ru.quest.models.Quest;
import ru.quest.models.QuestGame;
import ru.quest.models.Registration;
import ru.quest.models.User;
import ru.quest.services.QuestGameService;
import ru.quest.services.QuestService;
import ru.quest.services.RegistrationService;
import ru.quest.services.UserService;
import ru.quest.utils.ButtonsUtil;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static ru.quest.answers.constants.AnswerConstants.*;

@Service
public class ResultQuestGameAnswerService implements AnswerService {
    private static final String QUEST_GAME = "questGame";

    private final QuestService questService;
    private final QuestGameService questGameService;
    private final UserService userService;
    private final RegistrationService registrationService;

    public ResultQuestGameAnswerService(QuestService questService, QuestGameService questGameService, UserService userService, RegistrationService registrationService) {
        this.questService = questService;
        this.questGameService = questGameService;
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        System.out.println(dto);
        AnswerDTO answerDTO = new AnswerDTO();
        if (dto.getText().equals("/quests_results")) {
            List<String> questButtons = new ArrayList<>();
            List<Quest> quests = questService.getaAll();
            quests.sort(Collections.reverseOrder(Comparator.comparing(Quest::getDateTime)));
            quests.forEach(quest -> questButtons.add(quest.getQuestButton()));

            if (questButtons.isEmpty()) {
                answerDTO.getMessages().add(getSendMessage("?????? ?????????????????? ??????????????", dto.getChatId()));
            }
            else {
                String[] buttons = questButtons.toArray(new String[0]);
                answerDTO.getMessages().add(getSendMessage("???????????????? ??????????", buttons, dto.getChatId()));
            }
        }
        else if (dto.getText().matches(".+\\s\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}")) {
            Quest checkedQuest = questService.getaAll().stream()
                    .filter(quest -> quest.getQuestButton().equals(dto.getText())).findFirst().get();
            List<QuestGame> games = questGameService.getAllByQuestId(checkedQuest.getId());

            if (games.isEmpty()) {
                answerDTO.getMessages().add(getSendMessage("?????? ?????????????????????? ?????????????????????? ?????????????? ????????????", dto.getChatId()));
                return answerDTO;
            }

            sendMessageForQuestGame(games, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(QUEST_GAME + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] params = dto.getText().split(" ", 2);
            long questGameId = Long.parseLong(params[0].split(":")[1]);
            int change = Integer.parseInt(params[1].split(":")[1]);
            QuestGame questGame = questGameService.get(questGameId);

            List<QuestGame> games = questGameService.getAllByQuestId(questGame.getQuestId());

            QuestGame game = games.stream().filter(thisGame -> thisGame.getId() == questGame.getId()).findFirst().get();
            int index = games.indexOf(game);

            if(index+change < 0) {
                index = games.size()-1;
            }
            else if (index+change > games.size()-1) {
                index = 0;
            }
            else {
                index = index+change;
            }
            sendMessageForQuestGame(games, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        return answerDTO;
    }
    private void sendMessageForQuestGame(List<QuestGame> games, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        QuestGame questGame = games.get(index);
        String questGameInfo = getQuestGameInfo(questGame, index+1, games.size());

        if (messageId == 0) {
            answerDTO.getMessages().add(getSendMessage(questGameInfo, false, getInlineKeyboardMarkup(questGame), chatId));
        }
        else {
            answerDTO.getEditMessages().add(getEditMessageText(questGameInfo, getInlineKeyboardMarkup(questGame),false, chatId, messageId));
        }
    }

    private String getQuestGameInfo(QuestGame questGame, int num, int count) {
        User user = userService.get(questGame.getUserId());
        Quest quest = questService.get(questGame.getQuestId());
        Registration registration = registrationService.get(quest.getId(), user.getId());
        String teamInfo = "";
        if (quest.getType().equals(QuestType.GROUP)) {
            teamInfo = "\n???????????????? ??????????????: \"" + registration.getTeamName() + "\"" +
                    "\n???????????????????? ???????????????????? ?? ??????????????: " + registration.getTeamMembersNumber();
        }
        Duration duration = Duration.between(questGame.getStartTime(), questGame.getEndTime());
        Duration result = duration.minusMinutes(questGame.getPoints()).plusMinutes(questGame.getPenalties());
        return "??????????: " + quest.getName() +
                "\n\n" + num + "/" + count +
                "\n\n????????????????????????: " + user.getFirstName() + " " + user.getLastName() + " (@" + user.getUserName() + ")" +
                "\n?????????? ????????????????: +" + registration.getPhoneNumber() +
                teamInfo +
                "\n\n?????????? ???????????? ?????????????????????? ????????????: " + questGame.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) +
                "\n?????????? ?????????????????? ?????????????????????? ????????????: " + questGame.getEndTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) +
                "\n\n?????????? ?????????????????????? ????????????: "
                + duration.toHoursPart() + " ??  "
                + duration.toMinutesPart() + " ??????  "
                + duration.toSecondsPart() + " ??????"
                + "\n\n?????????????? ??????????: " + questGame.getPoints()
                + "\n?????????????? ??????????????: " + questGame.getPenalties()
                + "\n\n??????????????????: "
                + result.toHoursPart() + " ??  "
                + result.toMinutesPart() + " ??????  "
                + result.toSecondsPart() + " ??????";
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(QuestGame questGame) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowQuestGame(questGame.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowQuestGame(questGame.getId(), 1)));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        return inlineKeyboardMarkup;
    }

    private String getButtonDataToShowQuestGame(long questGameId, int index) {
        return QUEST_GAME + ":" + questGameId + " " + CHANGE_INDEX + ":" + index;
    }
}
