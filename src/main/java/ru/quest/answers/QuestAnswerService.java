package ru.quest.answers;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestAdminBot;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.RegistrationStatus;
import ru.quest.models.*;
import ru.quest.services.*;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.KhantyMansiyskDateTime;
import ru.quest.utils.LocationUtil;
import ru.quest.utils.ReservedCharacters;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.quest.answers.AnswerConstants.*;
import static ru.quest.answers.EditTaskAnswerService.*;

@Service
public class QuestAnswerService implements AnswerService{
    private static final String REGISTER = "Зарегистрироваться";
    private static final String ENTER_TEAM_NAME = "Введите название вашей команды";
    private static final String ENTER_TEAM_MEMBERS_NUMBER = "Введите количество участников в команде";
    public static final String GET_TASKS = "Получить задания";
    private static final String CHOOSE_TASK = "Выбрать задание";
    private static final String COMPLETE_TASK = "Выполнить задание";
    private static final String CONFIRM_LOCATION = "Подтвердить местоположение";
    private static final String COMPLETE_QUEST = "Завершить квест";
    private static final String SEND_ANSWER = "Отправьте свой ответ на задание";
    private static final String GET_HINT = "Получить подсказку";
    private static final String SURRENDER = "Сдаться";
    private static final String NEW_REG_NOTIFICATION = "Появилась новая заявка на квест";

    private final UserService userService;
    private final QuestService questService;
    private final TaskService taskService;
    private final QuestGameService questGameService;
    private final TaskCompletingService taskCompletingService;
    private final RegistrationService registrationService;
    private final LocationService locationService;
    private final LastAnswerService lastAnswerService;
    private final QuestAdminBot adminBot;

    private final Map<Long, QuestGame> games;
    private final Map<Long, Registration> registrations;

    public QuestAnswerService(UserService userService, QuestService questService, TaskService taskService, QuestGameService questGameService, TaskCompletingService taskCompletingService, RegistrationService registrationService, LocationService locationService, LastAnswerService lastAnswerService, @Lazy QuestAdminBot adminBot) {
        this.userService = userService;
        this.questService = questService;
        this.taskService = taskService;
        this.questGameService = questGameService;
        this.taskCompletingService = taskCompletingService;
        this.registrationService = registrationService;
        this.locationService = locationService;
        this.lastAnswerService = lastAnswerService;
        this.adminBot = adminBot;
        games = new HashMap<>();
        questGameService.getActiveQuestGames().forEach(questGame -> games.put(questGame.getUserId(), questGame));
        registrations = new HashMap<>();
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();
        if (dto.getText().equals("/quests")) {
            List<Quest> quests = questService.getaAll();
            String[] buttons = new String[quests.size()];
            for (int i = 0; i < quests.size(); i++) {
                buttons[i] = getQuestButton(quests.get(i));
            }
            answerDTO.getMessages().add(getSendMessage("Выберите квест", buttons, dto.getChatId()));
        }
        else if (dto.getText().matches(".+\\s\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}")) {
            Quest checkedQuest = questService.getaAll().stream()
                    .filter(quest -> getQuestButton(quest).equals(dto.getText())).findFirst().get();

            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(REGISTER, REGISTER + ":" + checkedQuest.getId()));
            answerDTO.getMessages().add(getSendMessageWithInlineButtons(getQuestInfo(checkedQuest), buttons, 1, true, dto.getChatId()));
        }
        else if (dto.getText().matches(REGISTER + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            Registration registration = registrationService.get(questId, dto.getChatId());
            if (registration != null) {
                if (registration.getStatus() == null) {
                    answerDTO.getMessages().add(getSendMessage("Ваша заявка на квест \"" + quest.getName()+ "\" находится на рассмотрении у организаторов.", dto.getChatId()));
                }
                else if (registration.getStatus() == RegistrationStatus.APPROVED) {
                    answerDTO.getMessages().add(getSendMessage("Вы уже зарегистрированы на квест \"" + quest.getName()+ "\"", dto.getChatId()));
                }
                else {
                    answerDTO.getMessages().add(getSendMessage("Ваша заявка на квест \"" + quest.getName()+ "\" была отклонена организаторами.", dto.getChatId()));
                }
                return answerDTO;
            }
            switch (quest.getType()) {
                case GROUP -> {
                    registration = new Registration(dto.getChatId(), questId);
                    registrations.put(dto.getChatId(), registration);
                    answerDTO.getMessages().add(getSendMessage(ENTER_TEAM_NAME, dto.getChatId()));
                    lastAnswerService.addLastAnswer(ENTER_TEAM_NAME, dto.getChatId());
                }
                case INDIVIDUAL -> {
                    registration = new Registration(dto.getChatId(), questId);
                    registrationService.save(registration);
                    answerDTO.getMessages().add(getSendMessage("Ваша заявка на квест \"" + quest.getName()+ "\" отправлена организаторам.", dto.getChatId()));
                    sendNotificationToAdmin(NEW_REG_NOTIFICATION + " \"" + quest.getName() + "\"!");
                }
            }
        }
        else if (registrations.containsKey(dto.getChatId())
                && lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEAM_NAME)) {
            registrations.get(dto.getChatId()).setTeamName(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_TEAM_MEMBERS_NUMBER, dto.getChatId()));
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            lastAnswerService.addLastAnswer(ENTER_TEAM_MEMBERS_NUMBER, dto.getChatId());
        }
        else if (dto.getText().matches("\\d+")
                && registrations.containsKey(dto.getChatId())
                && lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEAM_MEMBERS_NUMBER)) {
            int membersNumber = Integer.parseInt(dto.getText());
            Registration registration = registrations.remove(dto.getChatId());
            registration.setTeamMembersNumber(membersNumber);
            registration = registrationService.save(registration);
            Quest quest = questService.get(registration.getQuestId());
            answerDTO.getMessages().add(getSendMessage("Ваша заявка на квест \"" + quest.getName()+ "\" отправлена организаторам.", dto.getChatId()));
            sendNotificationToAdmin(NEW_REG_NOTIFICATION + " \"" + quest.getName() + "\"!");
        }
        else if (dto.getText().matches("confirm" + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Registration registration = registrationService.get(questId, dto.getChatId());
            if (registration == null) {
                answerDTO.getMessages().add(getSendMessage("Вы не зарегистрированы на указанный квест", dto.getChatId()));
                return answerDTO;
            }
            registration.setConfirmed(true);
            registrationService.save(registration);
            answerDTO.getMessages().add(getSendMessage("Спасибо, что подтвердили своё участие в квесте!", dto.getChatId()));
        }
        else if (dto.getText().matches("reject" + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            registrationService.delete(questId, dto.getChatId());
            answerDTO.getMessages().add(getSendMessage("Вы отказались от участия в квесте \"" + quest.getName() + "\"." +
                    "\nЕсли всё же надумаете участвовать, зарегистрируйтесь на квест заново.", dto.getChatId()));
        }
        else if (dto.getText().matches(GET_TASKS + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            QuestGame questGame = questGameService.create(questId, dto.getChatId());
            games.put(dto.getChatId(), questGame);
            List<Task> tasks = getAvailableTasks(questId, questGame).stream().filter(task -> !task.isLast())
                    .collect(Collectors.toList());
            sendMessageForTask(tasks, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(QUEST_ID + ":\\d+ " + THIS_TASK + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] params = dto.getText().split(" ", 3);
            long questId = Long.parseLong(params[0].split(":")[1]);
            long taskId = Long.parseLong(params[1].split(":")[1]);
            int change = Integer.parseInt(params[2].split(":")[1]);

            List<Task> tasks = getAvailableTasks(questId, games.get(dto.getChatId())).stream().filter(task -> !task.isLast())
                    .collect(Collectors.toList());

            Task task = tasks.stream().filter(thisTask -> thisTask.getId() == taskId).findFirst().get();
            int index = tasks.indexOf(task);

            if(index+change < 0) {
                index = tasks.size()-1;
            }
            else if (index+change > tasks.size()-1) {
                index = 0;
            }
            else {
                index = index+change;
            }

            sendMessageForTask(tasks, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(CHOOSE_TASK + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);
            Task task = taskService.get(taskId);

            QuestGame questGame = games.get(dto.getChatId());
            if (taskCompletingService.isPresentActiveTaskCompleting(questGame.getId())) {
                answerDTO.getMessages().add(getSendMessage("Вы не можете выбрать другое задание. " +
                        "Сначала необходимо завершить уже начатое задание.", dto.getChatId()));
                return answerDTO;
            }

            TaskCompleting taskCompleting = new TaskCompleting();
            taskCompleting.setTaskId(taskId);
            taskCompleting.setQuestGameId(games.get(dto.getChatId()).getId());
            taskCompleting.setStartTime(KhantyMansiyskDateTime.now());
            taskCompletingService.save(taskCompleting);

            SendMessage sendMessage = getSendMessage("Задание: \n" + task.getText() + "\n\n*Подтвердите местоположение*", null, true, dto.getChatId());
            sendMessage.setReplyMarkup(ButtonsUtil.getLocationButton(CONFIRM_LOCATION));
            answerDTO.getMessages().add(sendMessage);
            lastAnswerService.addLastAnswer(CONFIRM_LOCATION + ":" + task.getId(), dto.getChatId());
        }
        else if (dto.getLocation() != null && lastAnswerService.readLastAnswer(dto.getChatId()).matches(CONFIRM_LOCATION + ":\\d+")) {
            long taskId = Long.parseLong(lastAnswerService.readLastAnswer(dto.getChatId()).split(":")[1]);
            Task task = taskService.get(taskId);
            boolean isTrueLocation = LocationUtil.compareLocations(task.getLocation(), dto.getLocation());
            if (isTrueLocation) {
                Location location = locationService.save(dto.getLocation());
                QuestGame questGame = games.get(dto.getChatId());
                TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);
                taskCompleting.setUserLocation(location);
                taskCompletingService.save(taskCompleting);
                String message = "Местоположение подтверждено! Теперь можно выполнять задание." +
                        "\n\nЗадание:\n" + task.getText();
                List<InlineButtonDTO> buttons = new ArrayList<>();
                buttons.add(new InlineButtonDTO(COMPLETE_TASK, COMPLETE_TASK + ":" + task.getId()));
                answerDTO.getMessages().add(getSendMessageWithInlineButtons(message, buttons, 1, false, dto.getChatId()));

                lastAnswerService.deleteLastAnswer(dto.getChatId());
            }
            else {
                answerDTO.getMessages().add(getSendMessage("Ваше местоположение не соответствует заданию", dto.getChatId()));
            }
        }
        else if (dto.getText().matches(COMPLETE_TASK + ":\\d+")) {
            answerDTO.getMessages().add(getSendMessage(SEND_ANSWER, dto.getChatId()));
            lastAnswerService.addLastAnswer(SEND_ANSWER + ":" + dto.getText().split(":")[1], dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(SEND_ANSWER+ ":\\d+")) {
            long taskId = Long.parseLong(lastAnswerService.readLastAnswer(dto.getChatId()).split(":")[1]);
            Task task = taskService.get(taskId);

            if (!dto.getText().equals(task.getAnswer())) {
                answerDTO.getMessages().add(getSendMessage("Ответ неверный. Попробуйте ещё раз", dto.getChatId()));
                return answerDTO;
            }
            lastAnswerService.deleteLastAnswer(dto.getChatId());

            QuestGame questGame = games.get(dto.getChatId());
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);
            taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
            taskCompletingService.save(taskCompleting);
            questGame.setPoints(questGame.getPoints()+15);
            questGameService.save(questGame);

            List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(dto.getChatId())).stream().filter(task1 -> !task1.isLast())
                    .collect(Collectors.toList());
            if (!tasks.isEmpty()) {
                answerDTO.getMessages().add(getSendMessage("Задание выполнено! Вы можете выбрать следующее задание.", dto.getChatId()));
                sendMessageForTask(tasks, 0, dto.getChatId(), 0, answerDTO);
            }
            else {
                tasks = getAvailableTasks(task.getQuestId(), questGame).stream().filter(Task::isLast)
                        .collect(Collectors.toList());

                if (tasks.size() == 1) {
                    Task lastTask = tasks.get(0);
                    SendMessage sendMessage = getSendMessage(getLastTaskInfo(lastTask), dto.getChatId());

                    InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();
                    List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                    buttonDTOList.add(new InlineButtonDTO(CHOOSE_TASK, CHOOSE_TASK + ":" + lastTask.getId()));
                    inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

                    sendMessage.setReplyMarkup(inlineKeyboardMarkup);
                    answerDTO.getMessages().add(getSendMessage("Задание выполнено! Вы можете выполнить *последнее задание*.", null, true, dto.getChatId()));
                    answerDTO.getMessages().add(sendMessage);
                }
                else {
                    List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                    buttonDTOList.add(new InlineButtonDTO(COMPLETE_QUEST, COMPLETE_QUEST));
                    answerDTO.getMessages().add(getSendMessageWithInlineButtons("Задания выполнены!", buttonDTOList, 1, false, dto.getChatId()));
                }
            }
        }
        else if (dto.getText().equals(COMPLETE_QUEST)) {
            QuestGame questGame = games.get(dto.getChatId());
            questGame.setOver(true);
            questGame.setEndTime(KhantyMansiyskDateTime.now());
            questGameService.save(questGame);
            games.remove(dto.getChatId());

            Duration duration = Duration.between(questGame.getStartTime(), questGame.getEndTime());
            Duration result = duration.minusMinutes(questGame.getPoints());
            answerDTO.getMessages().add(getSendMessage("Квест завершён! " +
                    "\n\nВремя выполнения: "
                            + duration.toHoursPart() + "ч "
                            + duration.toMinutesPart() + "мин "
                            + duration.toSecondsPart() + "сек"
                    + "\nНабрано очков: " + questGame.getPoints()
                    + "\nРезультат: "
                            + result.toHoursPart() + "ч "
                            + result.toMinutesPart() + "мин "
                            + result.toSecondsPart() + "сек" ,
                    dto.getChatId()));
        }
        return answerDTO;
    }

    private String getQuestButton(Quest quest) {
        return quest.getName() + " " + quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String getQuestInfo(Quest quest) {
        return "*" + quest.getName() + "*" +
                "\n\nДата и время начала квеста: " + ReservedCharacters.replace(quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) +
                "\n\n" + ReservedCharacters.replace(quest.getDescription());
    }

    private void sendMessageForTask(List<Task> tasks, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Task task = tasks.get(index);
        if (messageId == 0) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(getTaskInfo(task, index+1, tasks.size()));
            sendMessage.setParseMode("MarkdownV2");
            sendMessage.setReplyMarkup(getInlineKeyboardMarkup(task));

            answerDTO.getMessages().add(sendMessage);
            return;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        editMessageText.setText(getTaskInfo(task, index+1, tasks.size()));
        editMessageText.setParseMode("MarkdownV2");
        editMessageText.setReplyMarkup(getInlineKeyboardMarkup(task));

        answerDTO.getEditMessages().add(editMessageText);
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Task task) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(CHOOSE_TASK, CHOOSE_TASK + ":" + task.getId()));

        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowTask(task.getQuestId(), task.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowTask(task.getQuestId(), task.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        return inlineKeyboardMarkup;
    }

    private String getTaskInfo(Task task, int num, int count) {
        return "Доступные задания\n" +
                num + "/" + count +
                "\n\nЗадание: \"" + ReservedCharacters.replace(task.getText()) + "\"";
    }

    private String getLastTaskInfo(Task task) {
        return "Последнее задание: \"" + ReservedCharacters.replace(task.getText()) + "\"";
    }

    private List<Task> getAvailableTasks(long questId, QuestGame questGame) {
        List<Long> completedTasksIdList = taskCompletingService.getCompletedTaskIdList(questGame.getId());
        return taskService.getAllByQuestId(questId)
                .stream().filter(task -> !completedTasksIdList.contains(task.getId()))
                .collect(Collectors.toList());
    }

    private void sendNotificationToAdmin(String message) {
        userService.getAll().stream().filter(User::isAdmin)
                .forEach(user -> adminBot.sendTheMessage(getSendMessage(message, user.getId())));
    }
}
