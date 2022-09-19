package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestBot;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.QuestType;
import ru.quest.enums.RegistrationStatus;
import ru.quest.models.Quest;
import ru.quest.models.Registration;
import ru.quest.models.User;
import ru.quest.services.QuestService;
import ru.quest.services.RegistrationService;
import ru.quest.services.UserService;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.KhantyMansiyskDateTime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ru.quest.answers.constants.AnswerConstants.*;
import static ru.quest.answers.EditTaskAnswerService.QUEST_ID;

@Service
public class ApproveRegistrationAnswerService implements AnswerService {
    private static final String APPROVE = "Принять";
    private static final String DISAPPROVE = "Отклонить";
    private static final String THIS_REGISTRATION = "registration";
    private static final String NO_REGISTRATIONS = "Нет заявок ожидающих подтверждения";

    private final RegistrationService registrationService;
    private final QuestService questService;
    private final UserService userService;
    private final QuestBot questBot;

    public ApproveRegistrationAnswerService(RegistrationService registrationService, QuestService questService, UserService userService, QuestBot questBot) {
        this.registrationService = registrationService;
        this.questService = questService;
        this.userService = userService;
        this.questBot = questBot;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();
        if (dto.getText().equals("/registrations")) {
            List<String> questButtons = new ArrayList<>();
            questService.getaAllNotDeleted().forEach(quest -> {
                List<Registration> registrations = registrationService.getAllByQuestId(quest.getId())
                        .stream().filter(reg -> reg.getStatus() == null).toList();
                if (!registrations.isEmpty()) {
                    questButtons.add(quest.getName() + "(" + registrations.size() + ")");
                }
            });

            if (questButtons.isEmpty()) {
                answerDTO.getMessages().add(getSendMessage(NO_REGISTRATIONS, dto.getChatId()));
            }
            else {
                String[] buttons = questButtons.toArray(new String[0]);
                answerDTO.getMessages().add(getSendMessage("Выберите квест", buttons, dto.getChatId()));
            }
        }
        else if (dto.getText().matches(".+\\(\\d+\\)")) {
            String questName = dto.getText().split("\\(\\d+\\)")[0];
            Quest chosenQuest = questService.getaAllNotDeleted().stream().filter(quest -> quest.getName().equals(questName)).findFirst().get();
            List<Registration> registrations = registrationService.getAllByQuestId(chosenQuest.getId())
                    .stream().filter(reg -> reg.getStatus() == null).toList();
            sendMessageForRegistration(registrations, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(QUEST_ID + ":\\d+ " + THIS_REGISTRATION + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] params = dto.getText().split(" ", 3);
            long questId = Long.parseLong(params[0].split(":")[1]);
            long registrationId = Long.parseLong(params[1].split(":")[1]);
            int change = Integer.parseInt(params[2].split(":")[1]);

            List<Registration> registrations = registrationService.getAllByQuestId(questId);

            Registration registration = registrations.stream().filter(thisTask -> thisTask.getId() == registrationId).findFirst().get();
            int index = registrations.indexOf(registration);

            if(index+change < 0) {
                index = registrations.size()-1;
            }
            else if (index+change > registrations.size()-1) {
                index = 0;
            }
            else {
                index = index+change;
            }
            sendMessageForRegistration(registrations, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(APPROVE + ":\\d+")) {
            long registrationId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Registration registration = registrationService.get(registrationId);
            Quest quest = questService.get(registration.getQuestId());

            registration.setStatus(RegistrationStatus.APPROVED);

            LocalDateTime dateTime = KhantyMansiyskDateTime.now();
            Duration duration = Duration.between(dateTime, quest.getDateTime());
            if (duration.toMinutes() < 60 * 24) {
                registration.setConfirmed(true);
            }

            registrationService.save(registration);
            questBot.sendTheMessage(getSendMessage("Ваша заявка на квест \"" + quest.getName() + "\" подтверждена. Вы успешно зарегистрированы.", registration.getUserId()));
            answerDTO.getMessages().add(getSendMessage("Заявка подтверждена", dto.getChatId()));

            sendRegistrationToApprove(quest, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(DISAPPROVE + ":\\d+")) {
            long registrationId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Registration registration = registrationService.get(registrationId);
            registration.setStatus(RegistrationStatus.DISAPPROVED);
            registrationService.save(registration);
            Quest quest = questService.get(registration.getQuestId());
            questBot.sendTheMessage(getSendMessage("Ваша заявка на квест \"" + quest.getName() + "\" отклонена", registration.getUserId()));
            answerDTO.getMessages().add(getSendMessage("Заявка отклонена", dto.getChatId()));

            sendRegistrationToApprove(quest, dto.getChatId(), answerDTO);
        }
        return answerDTO;
    }

    private void sendRegistrationToApprove(Quest quest, long chatId, AnswerDTO answerDTO) {
        List<Registration> registrations = registrationService.getAllByQuestId(quest.getId())
                .stream().filter(reg -> reg.getStatus() == null).toList();
        if (registrations.isEmpty()) {
            answerDTO.getMessages().add(getSendMessage(NO_REGISTRATIONS, chatId));
        }
        else {
            sendMessageForRegistration(registrations, 0, chatId, 0, answerDTO);
        }
    }

    private void sendMessageForRegistration(List<Registration> registrations, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Registration registration = registrations.get(index);
        String registrationInfo = getRegistrationInfo(registration, index+1, registrations.size());

        if (messageId == 0) {
            answerDTO.getMessages().add(getSendMessage(registrationInfo, false, getInlineKeyboardMarkup(registration), chatId));
        }
        else {
            answerDTO.getEditMessages().add(getEditMessageText(registrationInfo, getInlineKeyboardMarkup(registration),false, chatId, messageId));
        }
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Registration registration) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(APPROVE, APPROVE + ":" + registration.getId()));
        buttonDTOList.add(new InlineButtonDTO(DISAPPROVE, DISAPPROVE + ":" + registration.getId()));

        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowRegistration(registration.getQuestId(), registration.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowRegistration(registration.getQuestId(), registration.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        return inlineKeyboardMarkup;
    }

    private String getButtonDataToShowRegistration(long questId, long registrationId, int index) {
        return QUEST_ID + ":" + questId + " " + THIS_REGISTRATION + ":" + registrationId + " " + CHANGE_INDEX + ":" + index;
    }

    private String getRegistrationInfo(Registration registration, int num, int count) {
        User user = userService.get(registration.getUserId());
        Quest quest = questService.get(registration.getQuestId());
        String teamInfo = "";
        if (quest.getType().equals(QuestType.GROUP)) {
            teamInfo = "\nНазвание команды: \"" + registration.getTeamName() + "\"" +
                    "\nКоличество участников в команде: " + registration.getTeamMembersNumber();
        }
        return "Квест: " + quest.getName() +
                "\n\n" + num + "/" + count +
                "\n\nПользователь: " + user.getFirstName() + " " + user.getLastName() + " (@" + user.getUserName() + ")" +
                "\nНомер телефона: +" + registration.getPhoneNumber() +
                teamInfo;
    }
}
