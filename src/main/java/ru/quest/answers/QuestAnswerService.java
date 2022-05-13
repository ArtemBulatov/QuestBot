package ru.quest.answers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestAdminBot;
import ru.quest.answers.AnswerService;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.AnswerType;
import ru.quest.enums.RegistrationStatus;
import ru.quest.models.*;
import ru.quest.services.*;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.KhantyMansiyskDateTime;
import ru.quest.utils.LocationUtil;
import ru.quest.utils.ReservedCharacters;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.quest.answers.AnswerConstants.*;
import static ru.quest.answers.EditTaskAnswerService.*;

@Service
public class QuestAnswerService implements AnswerService {
    private static final String REGISTER = "Зарегистрироваться";
    private static final String ENTER_TEAM_NAME = "Введите название вашей команды";
    private static final String ENTER_TEAM_MEMBERS_NUMBER = "Введите количество участников в команде";
    public static final String GET_TASKS = "Получить задания";
    private static final String CHOOSE_TASK = "Выбрать задание";
    private static final String COMPLETE_TASK = "Отправить ответ";
    private static final String CONFIRM_LOCATION = "Подтвердить местоположение";
    private static final String COMPLETE_QUEST = "Завершить квест";
    private static final String COMPLETE_QUEST_DATA = "/complete_quest";
    private static final String SEND_ANSWER = "Отправьте свой ответ на задание";
    private static final String GET_HINT = "Получить подсказку";
    private static final String SURRENDER = "Сдаться";
    private static final String NEW_REG_NOTIFICATION = "Появилась новая заявка на квест";

    private final UserService userService;
    private final QuestService questService;
    private final TaskService taskService;
    private final HintService hintService;
    private final QuestGameService questGameService;
    private final TaskCompletingService taskCompletingService;
    private final RegistrationService registrationService;
    private final LocationService locationService;
    private final PhotoService photoService;
    private final LastAnswerService lastAnswerService;
    private final QuestAdminBot adminBot;

    private final Map<Long, QuestGame> games;
    private final Map<Long, Registration> registrations;

    @Value("${user.bot.token}")
    private String botToken;

    public QuestAnswerService(
            UserService userService,
            QuestService questService,
            TaskService taskService,
            HintService hintService,
            QuestGameService questGameService,
            TaskCompletingService taskCompletingService,
            RegistrationService registrationService,
            LocationService locationService,
            PhotoService photoService, LastAnswerService lastAnswerService,
            @Lazy QuestAdminBot adminBot
    ) {
        this.userService = userService;
        this.questService = questService;
        this.taskService = taskService;
        this.hintService = hintService;
        this.questGameService = questGameService;
        this.taskCompletingService = taskCompletingService;
        this.registrationService = registrationService;
        this.locationService = locationService;
        this.photoService = photoService;
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
                buttons[i] = quests.get(i).getQuestButton();
            }
            answerDTO.getMessages().add(getSendMessage("Выберите квест", buttons, dto.getChatId()));
        }
        else if (dto.getText().matches(".+\\s\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}")) {
            Quest checkedQuest = questService.getaAll().stream()
                    .filter(quest -> quest.getQuestButton().equals(dto.getText())).findFirst().get();

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
            sendMessageForTask(questGame.getPoints(), tasks, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
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

            sendMessageForTask(games.get(dto.getChatId()).getPoints(), tasks, index, dto.getChatId(), dto.getMessageId(), answerDTO);
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
                buttons.add(new InlineButtonDTO(GET_HINT, GET_HINT + ":" + task.getId()));
                buttons.add(new InlineButtonDTO(SURRENDER, SURRENDER + ":" + task.getId()));
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
        else if (dto.getText().equals(COMPLETE_QUEST_DATA)) {
            if (!games.containsKey(dto.getChatId())) {
                answerDTO.getMessages().add(getSendMessage("Нет квеста для завершения", dto.getChatId()));
                return answerDTO;
            }
            QuestGame questGame = games.get(dto.getChatId());
            questGame.setOver(true);
            questGame.setEndTime(KhantyMansiyskDateTime.now());
            questGameService.save(questGame);
            games.remove(dto.getChatId());

            Duration duration = Duration.between(questGame.getStartTime(), questGame.getEndTime());
            Duration result = duration.minusMinutes(questGame.getPoints()).plusMinutes(questGame.getPenalties());
            answerDTO.getMessages().add(getSendMessage("Квест завершён! " +
                    "\n\nВремя выполнения: "
                            + duration.toHoursPart() + "ч "
                            + duration.toMinutesPart() + "мин "
                            + duration.toSecondsPart() + "сек"
                    + "\nНабрано очков: " + questGame.getPoints()
                    + "\nНабрано штрафов: " + questGame.getPenalties()
                    + "\n*Результат: "
                            + result.toHoursPart() + " ч  "
                            + result.toMinutesPart() + " мин  "
                            + result.toSecondsPart() + " сек*" ,
                    null,
                    true,
                    dto.getChatId()));
        }
        else if (dto.getText().matches(GET_HINT + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);

            QuestGame questGame = games.get(dto.getChatId());
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (taskCompleting.getUsedHints().size() > 2) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Вы уже использовали 3 возможные подсказки!", 5, dto.getCallbackQueryId()));
                return answerDTO;
            }

            if (questGame.getPoints() < 5) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Недостаточно баллов для получения подсказки", 5, dto.getCallbackQueryId()));
                return answerDTO;
            }

            Set<Long> usedHintIds = new HashSet<>();
            taskCompleting.getUsedHints().forEach(completing -> usedHintIds.add(completing.getId()));

            List<Hint> allHints = hintService.getAllByTaskId(taskId);

            if (allHints.isEmpty()) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Для этого задания подсказок нет", 3, dto.getCallbackQueryId()));
                return answerDTO;
            }

            List<Hint> hints = allHints.stream().filter(hint -> !usedHintIds.contains(hint.getId())).toList();
            Hint hint = hints.get(0);

            answerDTO.getMessages().add(getSendMessage(hint.getText(), dto.getChatId()));

            taskCompleting.getUsedHints().add(hint);
            taskCompletingService.save(taskCompleting);

            questGame.setPoints(questGame.getPoints() - 5);
            questGameService.save(questGame);
        }
        else if (dto.getText().matches(SURRENDER + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);
            Task task = taskService.get(taskId);

            QuestGame questGame = games.get(dto.getChatId());
            questGame.setPenalties(questGame.getPenalties() + 15);

            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);
            taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
            taskCompletingService.save(taskCompleting);

            List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(dto.getChatId()))
                    .stream().filter(task1 -> !task1.isLast()).collect(Collectors.toList());

            if (tasks.isEmpty()) {
                questGame.setOver(true);
                questGame.setEndTime(KhantyMansiyskDateTime.now());
            }
            questGameService.save(questGame);

            String message = "";
            completeTaskMessage(questGame.getPoints(), message, tasks, task.getQuestId(), questGame, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(CONFIRM_PHOTO + ":\\d+II" + USER + ":\\d+")) {
            String[] params = dto.getText().split("II", 2);
            long taskId = Long.parseLong(params[0].split(":", 2)[1]);
            long userId = Long.parseLong(params[1].split(":", 2)[1]);

            Task task = taskService.get(taskId);

            QuestGame questGame = games.get(userId);
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (!taskCompleting.isAnswered()) {
                taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
                taskCompleting.setAnswered(true);
                taskCompletingService.save(taskCompleting);
                questGame.setPoints(questGame.getPoints()+15);
                questGameService.save(questGame);

                List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(userId))
                        .stream().filter(task1 -> !task1.isLast()).toList();

                completeTaskMessage(questGame.getPoints(), "Задание выполнено!", tasks, task.getQuestId(), questGame, userId, answerDTO);
            }
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(SEND_ANSWER+ ":\\d+")) {
            long taskId = Long.parseLong(lastAnswerService.readLastAnswer(dto.getChatId()).split(":")[1]);
            Task task = taskService.get(taskId);

            if (task.getAnswerType() == AnswerType.PHOTO) {
                if (dto.getPhotoSizeList().isEmpty()) {
                    answerDTO.getMessages().add(getSendMessage("Ответом на это задание должно быть фото", dto.getChatId()));
                    return answerDTO;
                }
                Photo photo = photoService.getSavedPhotoFromDto(dto.getPhotoSizeList(), botToken);

                adminBot.sendThePhoto(getMessageToCheckPhoto(photo, task, dto.getChatId(), getAdminID()));
                answerDTO.getMessages().add(getSendMessage("Ваше фото отправлено на проверку", dto.getChatId()));

                lastAnswerService.deleteLastAnswer(dto.getChatId());
                return answerDTO;
            }

            if (!dto.getText().equals(task.getAnswer())) {
                answerDTO.getMessages().add(getSendMessage("Ответ неверный. Попробуйте ещё раз", dto.getChatId()));
                return answerDTO;
            }
            lastAnswerService.deleteLastAnswer(dto.getChatId());

            QuestGame questGame = games.get(dto.getChatId());
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);
            taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
            taskCompleting.setAnswered(true);
            taskCompletingService.save(taskCompleting);
            questGame.setPoints(questGame.getPoints()+15);
            questGameService.save(questGame);

            List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(dto.getChatId()))
                    .stream().filter(task1 -> !task1.isLast()).toList();

            completeTaskMessage(questGame.getPoints(), "Задание выполнено!", tasks, task.getQuestId(), questGame, dto.getChatId(), answerDTO);
        }
        return answerDTO;
    }

    private void completeTaskMessage(int points, String message, List<Task> tasks, long questId, QuestGame questGame, long chatId, AnswerDTO answerDTO) {
        if (!message.isEmpty()) {
            answerDTO.getMessages().add(getSendMessage(message, chatId));
        }
        if (!tasks.isEmpty()) {
            answerDTO.getMessages().add(getSendMessage("Вы можете выбрать следующее задание.", chatId));
            sendMessageForTask(points, tasks, 0, chatId, 0, answerDTO);
        }
        else {
            tasks = getAvailableTasks(questId, questGame).stream().filter(Task::isLast)
                    .collect(Collectors.toList());

            if (tasks.size() == 1) {
                Task lastTask = tasks.get(0);
                SendMessage sendMessage = getSendMessage(getLastTaskInfo(lastTask), chatId);

                InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();
                List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                buttonDTOList.add(new InlineButtonDTO(CHOOSE_TASK, CHOOSE_TASK + ":" + lastTask.getId()));
                inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

                sendMessage.setReplyMarkup(inlineKeyboardMarkup);
                answerDTO.getMessages().add(getSendMessage("Вы можете выполнить *последнее задание*.", null, true, chatId));
                answerDTO.getMessages().add(sendMessage);
            }
            else {
                List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                buttonDTOList.add(new InlineButtonDTO(COMPLETE_QUEST, COMPLETE_QUEST_DATA));
                answerDTO.getMessages().add(getSendMessageWithInlineButtons("Задания закончились!", buttonDTOList, 1, false, chatId));
            }
        }
    }

    private String getQuestInfo(Quest quest) {
        return "*" + quest.getName() + "*" +
                "\n\nДата и время начала квеста: " + ReservedCharacters.replace(quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) +
                "\n\n" + ReservedCharacters.replace(quest.getDescription());
    }

    private void sendMessageForTask(int points, List<Task> tasks, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Task task = tasks.get(index);
        if (messageId == 0) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(getTaskInfo(points, task, index+1, tasks.size()));
            sendMessage.setParseMode("MarkdownV2");
            sendMessage.setReplyMarkup(getInlineKeyboardMarkup(task));

            answerDTO.getMessages().add(sendMessage);
            return;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        editMessageText.setText(getTaskInfo(points, task, index+1, tasks.size()));
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

    private String getTaskInfo(int points, Task task, int num, int count) {
        return "Ваши баллы: " + points +
                "\n\nДоступные задания\n" +
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

    private SendPhoto getMessageToCheckPhoto(Photo photo, Task task, long userId, long adminId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(adminId));
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photo.getBytes()), photo.getName()));
        sendPhoto.setCaption("Проверьте фото для задания: \n\n" + task.getText());

        List<InlineButtonDTO> buttons = new ArrayList<>();
        buttons.add(new InlineButtonDTO(CONFIRM_PHOTO, CONFIRM_PHOTO + ":" + task.getId() + "II" + USER + ":" + userId));
        buttons.add(new InlineButtonDTO(NOT_CONFIRM_PHOTO, NOT_CONFIRM_PHOTO + ":" + task.getId() + "II" + USER + ":" + userId));
        sendPhoto.setReplyMarkup(ButtonsUtil.getInlineButtons(buttons, 2));
        return sendPhoto;
    }

    private long getAdminID() {
        return 711842183;
    }
}
