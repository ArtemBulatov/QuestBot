package ru.quest.answers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestAdminBot;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.quest.answers.constants.AnswerConstants.*;
import static ru.quest.answers.EditTaskAnswerService.*;

@Service
public class QuestAnswerService implements AnswerService {
    private static final String REGISTER = "Зарегистрироваться";
    private static final String ENTER_PHONE_NUMBER = "Введите номер телефона, начиная с 7";
    private static final String ENTER_TEAM_NAME = "Введите название вашей команды";
    private static final String ENTER_TEAM_MEMBERS_NUMBER = "Введите количество участников в команде";
    public static final String GET_TASKS = "Получить задания";
    public static final String BEGIN_QUEST = "Начать квест";
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
    private final PrologueService prologueService;
    private final EpilogueService epilogueService;
    private final QuestGameService questGameService;
    private final TaskCompletingService taskCompletingService;
    private final RegistrationService registrationService;
    private final LocationService locationService;
    private final PhotoService photoService;
    private final ValidationService validationService;
    private final LastAnswerService lastAnswerService;
    private final QuestAdminBot adminBot;
    private final LocationUtil locationUtil;

    private final Map<Long, QuestGame> games;
    private final Map<Long, Registration> registrations;

    @Value("${user.bot.token}")
    private String botToken;

    public QuestAnswerService(
            UserService userService,
            QuestService questService,
            TaskService taskService,
            HintService hintService,
            PrologueService prologueService,
            EpilogueService epilogueService,
            QuestGameService questGameService,
            TaskCompletingService taskCompletingService,
            RegistrationService registrationService,
            LocationService locationService,
            PhotoService photoService,
            ValidationService validationService,
            LastAnswerService lastAnswerService,
            @Lazy QuestAdminBot adminBot,
            LocationUtil locationUtil) {
        this.userService = userService;
        this.questService = questService;
        this.taskService = taskService;
        this.hintService = hintService;
        this.prologueService = prologueService;
        this.epilogueService = epilogueService;
        this.questGameService = questGameService;
        this.taskCompletingService = taskCompletingService;
        this.registrationService = registrationService;
        this.locationService = locationService;
        this.photoService = photoService;
        this.validationService = validationService;
        this.lastAnswerService = lastAnswerService;
        this.adminBot = adminBot;
        this.locationUtil = locationUtil;
        games = new HashMap<>();
        questGameService.getActiveQuestGames().forEach(questGame -> games.put(questGame.getUserId(), questGame));
        registrations = new HashMap<>();
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if(dto.getText().contains("/start")) {
            answerDTO.getMessages().add(getSendMessage("Для работы с ботом используйте меню", dto.getChatId()));
        }
        else if (dto.getText().equals("/quests")) {
            if (games.containsKey(dto.getChatId())) {
                return answerDTO;
            }

            LocalDateTime dateTime = KhantyMansiyskDateTime.now();
            List<Quest> quests = questService.getaAllNotDeleted().stream()
                    .filter(quest -> quest.getDateTime().isAfter(dateTime))
                    .collect(Collectors.toList());

            if (quests.isEmpty()) {
                answerDTO.getMessages().add(getSendMessage("Квестов пока нет", dto.getChatId()));
                return answerDTO;
            }

            if (quests.size() > 1) {
                quests.sort(Comparator.comparing(Quest::getDateTime));
            }

            String[] buttons = new String[quests.size()];
            for (int i = 0; i < quests.size(); i++) {
                buttons[i] = quests.get(i).getQuestButton();
            }
            answerDTO.getMessages().add(getSendMessage("Выберите квест", buttons, dto.getChatId()));
        }
        else if (dto.getText().matches(".+\\s\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}")) {
            Quest checkedQuest = questService.getaAllNotDeleted().stream()
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

            registration = new Registration(dto.getChatId(), questId);
            registrations.put(dto.getChatId(), registration);

            switch (quest.getType()) {
                case GROUP -> {
                    answerDTO.getMessages().add(getSendMessage(ENTER_TEAM_NAME, dto.getChatId()));
                    lastAnswerService.addLastAnswer(ENTER_TEAM_NAME, dto.getChatId());
                }
                case INDIVIDUAL -> {
                    answerDTO.getMessages().add(getSendMessage(ENTER_PHONE_NUMBER, dto.getChatId()));
                    lastAnswerService.addLastAnswer(ENTER_PHONE_NUMBER, dto.getChatId());
                }
            }
        }
        else if (dto.getText().matches("\\d+")
                && registrations.containsKey(dto.getChatId())
                && lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEAM_MEMBERS_NUMBER)) {
            int membersNumber = Integer.parseInt(dto.getText());
            registrations.get(dto.getChatId()).setTeamMembersNumber(membersNumber);

            answerDTO.getMessages().add(getSendMessage(ENTER_PHONE_NUMBER, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_PHONE_NUMBER, dto.getChatId());
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_PHONE_NUMBER)
                && registrations.containsKey(dto.getChatId())) {
            Registration registration = registrations.get(dto.getChatId());

            if (registration != null) {
                String phoneNumber = dto.getText();

                if (!validationService.isPhoneValid(phoneNumber)) {
                    answerDTO.getMessages().add(getSendMessage("Введите только цифры, начиная с 7", dto.getChatId()));
                    return answerDTO;
                }

                registration.setPhoneNumber(phoneNumber);
                registrationService.save(registration);

                lastAnswerService.deleteLastAnswer(dto.getChatId());

                Quest quest = questService.get(registration.getQuestId());
                answerDTO.getMessages().add(getSendMessage("Ваша заявка на квест \"" + quest.getName()+ "\" отправлена организаторам.", dto.getChatId()));
                sendNotificationToAdmin(NEW_REG_NOTIFICATION + " \"" + quest.getName() + "\"!");
            }
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
        else if (dto.getText().matches(BEGIN_QUEST + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Prologue prologue = prologueService.findByQuestId(questId);

            QuestGame questGame = questGameService.create(questId, dto.getChatId());
            games.put(dto.getChatId(), questGame);

            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(GET_TASKS, GET_TASKS + ":" + questId));

            if (prologue.getPhoto() == null) {
                answerDTO.getMessages().add(getSendMessageWithInlineButtons(prologue.getText(), buttons, 1, false, dto.getChatId()));
            }
            else {
                answerDTO.getPhotoMessages().add(getSendPhoto(prologue.getText(), prologue.getPhoto(), false, ButtonsUtil.getInlineButtons(buttons, 1), dto.getChatId()));
            }
        }
        else if (dto.getText().matches(GET_TASKS + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            QuestGame questGame = games.get(dto.getChatId());

            if (questGame == null) {
                questGame = questGameService.create(questId, dto.getChatId());
                games.put(dto.getChatId(), questGame);
            }

            List<Task> tasks = getAvailableTasks(questId, questGame).stream().filter(task -> !task.isLast())
                    .collect(Collectors.toList());
            sendMessageForTask(questGame, tasks, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
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

            sendMessageForTask(games.get(dto.getChatId()), tasks, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(CHOOSE_TASK + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);
            Task task = taskService.get(taskId);

            QuestGame questGame = games.get(dto.getChatId());
            if (taskCompletingService.isPresentActiveTaskCompleting(questGame.getId())) {
                answerDTO.getMessages().add(getSendMessage("Вы не можете выбрать другое задание. " +
                        "Сначала необходимо завершить уже начатое задание.", dto.getChatId()));
                sendMessageForTask(task, questGame, dto.getChatId(), answerDTO);
                return answerDTO;
            }

            TaskCompleting taskCompleting = new TaskCompleting();
            taskCompleting.setTaskId(taskId);
            taskCompleting.setQuestGameId(games.get(dto.getChatId()).getId());
            taskCompleting.setStartTime(KhantyMansiyskDateTime.now());
            taskCompletingService.save(taskCompleting);

            sendMessageForTask(task, questGame, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(COMPLETE_TASK + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);
            QuestGame questGame = games.get(dto.getChatId());
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (taskCompleting.getEndTime() != null) {
                return answerDTO;
            }

            if (taskCompleting.getUserLocation() == null) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Необходимо подтвердить местоположение", 3, dto.getCallbackQueryId()));
                return answerDTO;
            }

            answerDTO.getMessages().add(getSendMessage(SEND_ANSWER, dto.getChatId()));
            lastAnswerService.addLastAnswer(SEND_ANSWER + ":" + taskId, dto.getChatId());
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

            answerDTO.getMessages().add(getSendMessage(getQuestGameResultMessage(questGame, questService.get(questGame.getQuestId())),
                    null, true, dto.getChatId()));
        }
        else if (dto.getText().matches(SURRENDER + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);
            Task task = taskService.get(taskId);

            QuestGame questGame = games.get(dto.getChatId());

            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (taskCompleting.getEndTime() != null) {
                return answerDTO;
            }

            taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
            taskCompletingService.save(taskCompleting);

            questGame.setPenalties(questGame.getPenalties() + 15);

            List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(dto.getChatId()))
                    .stream().filter(task1 -> !task1.isLast()).collect(Collectors.toList());

            questGameService.save(questGame);

            String message = "Вы сдались и не выполнили задание. Вам назначен штраф.";
            completeTaskMessage(questGame, message, tasks, task.getQuestId(), questGame, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(GET_HINT + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":")[1]);

            QuestGame questGame = games.get(dto.getChatId());
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (taskCompleting.getEndTime() != null) {
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

            if (hints.isEmpty()) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Вы уже использовали все возможные подсказки!", 3, dto.getCallbackQueryId()));
                return answerDTO;
            }

            if (questGame.getPoints() < 5) {
                answerDTO.getCallbackQueries().add(getAnswerCallbackQuery("Недостаточно баллов для получения подсказки", 5, dto.getCallbackQueryId()));
                return answerDTO;
            }

            Hint hint = hints.get(0);

            if (hint.getHintsTask() != null) {
                String hintsTask = "Для получения подсказки: " + hint.getHintsTask();
                answerDTO.getMessages().add(getSendMessage(hintsTask, dto.getChatId()));
                lastAnswerService.addLastAnswer(GET_HINT + HINT + ":" + hint.getId(), dto.getChatId());
                return answerDTO;
            }

            answerDTO.getMessages().add(getSendMessage("Подсказка: " + hint.getText(), dto.getChatId()));

            taskCompleting.getUsedHints().add(hint);
            taskCompletingService.save(taskCompleting);

            questGame.setPoints(questGame.getPoints() - 5);
            questGameService.save(questGame);
        }
        else if (dto.getText().matches(CONFIRM_PHOTO + ":\\d+" + TASK + USER + ":\\d+")) {
            String[] params = dto.getText().split(TASK, 2);
            long taskId = Long.parseLong(params[0].split(":", 2)[1]);
            long userId = Long.parseLong(params[1].split(":", 2)[1]);

            QuestGame questGame = games.get(userId);
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (!taskCompleting.isAnswered()) {
                taskCompleting.setEndTime(KhantyMansiyskDateTime.now());
                taskCompleting.setAnswered(true);
                taskCompletingService.save(taskCompleting);
                questGame.setPoints(questGame.getPoints()+15);
                questGameService.save(questGame);

                Task task = taskService.get(taskId);

                List<Task> tasks = getAvailableTasks(task.getQuestId(), games.get(userId))
                        .stream().filter(task1 -> !task1.isLast()).toList();

                completeTaskMessage(questGame, task.getTrueAnswer(), tasks, task.getQuestId(), questGame, userId, answerDTO);
            }
        }
        else if (dto.getText().matches(NOT_CONFIRM_PHOTO + ":\\d+" + TASK + USER + ":\\d+")) {
            String[] params = dto.getText().split(TASK, 2);
            long taskId = Long.parseLong(params[0].split(":", 2)[1]);
            long userId = Long.parseLong(params[1].split(":", 2)[1]);

            QuestGame questGame = games.get(userId);
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);

            if (!taskCompleting.isAnswered()) {
                Task task = taskService.get(taskCompleting.getTaskId());
                if (task.getFalseAnswerPhoto() == null) {
                    answerDTO.getMessages().add(getSendMessage(task.getFalseAnswer(), userId));
                }
                else {
                    answerDTO.getPhotoMessages().add(getSendPhoto(task.getPhoto(), userId));
                }
                lastAnswerService.addLastAnswer(SEND_ANSWER + ":" + taskId, userId);
            }
        }
        else if (dto.getText().matches(CONFIRM_PHOTO + ":\\d+" + HINT + USER + ":\\d+")) {
            String[] params = dto.getText().split(HINT, 2);
            long hintId = Long.parseLong(params[0].split(":", 2)[1]);
            long userId = Long.parseLong(params[1].split(":", 2)[1]);

            Hint hint = hintService.get(hintId);
            Task task = taskService.get(hint.getTaskId());
            QuestGame questGame = games.get(userId);
            TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), hint.getTaskId());

            if (taskCompleting.getUsedHints().stream().anyMatch(thisHint -> thisHint.getId() == hintId)) {
                return answerDTO;
            }

            answerDTO.getMessages().add(getSendMessage("Фото принято", userId));
            answerDTO.getMessages().add(getSendMessage("Подсказка: " + hint.getText(), userId));

            taskCompleting.getUsedHints().add(hint);
            taskCompletingService.save(taskCompleting);

            questGame.setPoints(questGame.getPoints() - 5);
            questGameService.save(questGame);

            sendMessageForTask(task, questGame, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(NOT_CONFIRM_PHOTO + ":\\d+" + HINT + USER + ":\\d+")) {
            String[] params = dto.getText().split(HINT, 2);
            long hintId = Long.parseLong(params[0].split(":", 2)[1]);
            long userId = Long.parseLong(params[1].split(":", 2)[1]);
            Hint hint = hintService.get(hintId);

            answerDTO.getMessages().add(getSendMessage("Фото не принято. Попробуйте снова", userId));
            answerDTO.getMessages().add(getSendMessage(hint.getHintsTask(), dto.getChatId()));
            lastAnswerService.addLastAnswer(GET_HINT + HINT + ":" + hint.getId(), dto.getChatId());
        }
        else if (dto.getText().matches(CONFIRM_LOCATION + ":\\d+")) {
            lastAnswerService.addLastAnswer(dto.getText(), dto.getChatId());
            SendMessage sendMessage = getSendMessage("Отправьте местоположение", dto.getChatId());
            sendMessage.setReplyMarkup(ButtonsUtil.getLocationButton(SEND_MY_LOCATION));
            answerDTO.getMessages().add(sendMessage);
        }

        else if (registrations.containsKey(dto.getChatId())
                && lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEAM_NAME)) {
            registrations.get(dto.getChatId()).setTeamName(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_TEAM_MEMBERS_NUMBER, dto.getChatId()));
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            lastAnswerService.addLastAnswer(ENTER_TEAM_MEMBERS_NUMBER, dto.getChatId());
        }
        else if (dto.getLocation() != null && lastAnswerService.readLastAnswer(dto.getChatId()).matches(CONFIRM_LOCATION + ":\\d+")) {
            long taskId = Long.parseLong(lastAnswerService.readLastAnswer(dto.getChatId()).split(":")[1]);
            Task task = taskService.get(taskId);
            boolean isTrueLocation = locationUtil.compareLocations(task.getLocation(), dto.getLocation());
            if (isTrueLocation) {
                Location location = locationService.save(dto.getLocation());
                QuestGame questGame = games.get(dto.getChatId());
                TaskCompleting taskCompleting = taskCompletingService.get(questGame.getId(), taskId);
                taskCompleting.setUserLocation(location);
                taskCompletingService.save(taskCompleting);
                answerDTO.getMessages().add(getSendMessage("Местоположение подтверждено!", dto.getChatId()));
                sendMessageForTask(task, questGame, dto.getChatId(), answerDTO);
                lastAnswerService.deleteLastAnswer(dto.getChatId());
            }
            else {
                answerDTO.getMessages().add(getSendMessage("Ваше местоположение не соответствует заданию", dto.getChatId()));
            }
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(GET_HINT + HINT + ":\\d+")
                && !dto.getPhotoSizeList().isEmpty()) {
            long hintId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            Photo photo = photoService.getSavedPhotoFromDto(dto.getPhotoSizeList(), botToken);
            Hint hint = hintService.get(hintId);

            String message = "Проверьте фото для получения подсказки: \n\n" + hint.getHintsTask();

            userService.getAll()
                    .stream().filter(User::isAdmin)
                    .forEach(user -> adminBot.sendThePhoto(getMessageToCheckPhoto(photo, message, hintId, HINT, dto.getChatId(), user.getId())));
            answerDTO.getMessages().add(getSendMessage("Ваше фото отправлено на проверку", dto.getChatId()));
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

                String message = "Проверьте фото для задания: \n\n" + task.getText();

                userService.getAll()
                        .stream().filter(User::isAdmin)
                        .forEach(user -> adminBot.sendThePhoto(getMessageToCheckPhoto(photo, message, task.getId(), TASK, dto.getChatId(), user.getId())));
                answerDTO.getMessages().add(getSendMessage("Ваше фото отправлено на проверку", dto.getChatId()));

                lastAnswerService.deleteLastAnswer(dto.getChatId());
                return answerDTO;
            }

            if (!dto.getText().toLowerCase(Locale.ROOT).equals(task.getAnswer().toLowerCase(Locale.ROOT))) {
                if (task.getFalseAnswerPhoto() == null) {
                    answerDTO.getMessages().add(getSendMessage(task.getFalseAnswer(), dto.getChatId()));
                }
                else {
                    answerDTO.getPhotoMessages().add(getSendPhoto(task.getFalseAnswerPhoto(), dto.getChatId()));
                }
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

            completeTaskMessage(questGame, task.getTrueAnswer(), tasks, task.getQuestId(), questGame, dto.getChatId(), answerDTO);
        }
        return answerDTO;
    }

    private void completeTaskMessage(QuestGame game, String message, List<Task> tasks, long questId, QuestGame questGame, long chatId, AnswerDTO answerDTO) {
        if (!message.isEmpty()) {
            answerDTO.getMessages().add(getSendMessage(message, chatId));
        }
        if (!tasks.isEmpty()) {
            answerDTO.getMessages().add(getSendMessage("Вы можете выбрать следующее задание.", chatId));
            sendMessageForTask(game, tasks, 0, chatId, 0, answerDTO);
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
                answerDTO.getMessages().add(getSendMessage("Вы можете выполнить *последнее задание*\\.", null, true, chatId));
                answerDTO.getMessages().add(sendMessage);
            }
            else {
                List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                buttonDTOList.add(new InlineButtonDTO(COMPLETE_QUEST, COMPLETE_QUEST_DATA));

                Epilogue epilogue = epilogueService.findByQuestId(questId);
                if (epilogue == null) {
                    answerDTO.getMessages().add(getSendMessageWithInlineButtons("Задания закончились!", buttonDTOList, 1, false, chatId));
                }
                else if (epilogue.getPhoto() == null) {
                    answerDTO.getMessages().add(getSendMessageWithInlineButtons(epilogue.getText(), buttonDTOList, 1, false, chatId));
                }
                else {
                    answerDTO.getPhotoMessages().add(getSendPhoto(epilogue.getText(), epilogue.getPhoto(), false, ButtonsUtil.getInlineButtons(buttonDTOList, 1), chatId));
                }

            }
        }
    }

    private String getQuestInfo(Quest quest) {
        return "*" + quest.getName() + "*" +
                "\n\nДата и время начала квеста: " + ReservedCharacters.replace(quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) +
                "\n\n" + ReservedCharacters.replace(quest.getDescription());
    }

    private void sendMessageForTask(QuestGame game, List<Task> tasks, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Task task = tasks.get(index);
        String taskInfo = getTaskInfo(game, task, index+1, tasks.size());

        if (messageId != 0) {
            answerDTO.getDeleteMessages().add(getDeleteMessage(chatId, messageId));
        }
        if (task.getPhoto() == null) {
            answerDTO.getMessages().add(getSendMessage(taskInfo, false, getInlineKeyboardMarkupForTaskList(task), chatId));
        }
        else {
            answerDTO.getPhotoMessages().add(getSendPhoto(taskInfo, task.getPhoto(), false, getInlineKeyboardMarkupForTaskList(task), chatId));
        }
    }

    private void sendMessageForTask(Task task, QuestGame game, long chatId, AnswerDTO answerDTO) {
        String message = getTaskInfo(game, task);
        if (task.getPhoto() == null) {
            answerDTO.getMessages().add(getSendMessage(message, false, getInlineKeyboardMarkupForChosenTask(task), chatId));
        }
        else {
            answerDTO.getPhotoMessages().add(getSendPhoto(message, task.getPhoto(), false, getInlineKeyboardMarkupForChosenTask(task), chatId));
        }
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkupForTaskList(Task task) {
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

    private InlineKeyboardMarkup getInlineKeyboardMarkupForChosenTask(Task task) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();
        List<InlineButtonDTO> buttons = new ArrayList<>();
        buttons.add(new InlineButtonDTO(COMPLETE_TASK, COMPLETE_TASK + ":" + task.getId()));
        buttons.add(new InlineButtonDTO(GET_HINT, GET_HINT + ":" + task.getId()));
        buttons.add(new InlineButtonDTO(SURRENDER, SURRENDER + ":" + task.getId()));
        buttons.add(new InlineButtonDTO(CONFIRM_LOCATION, CONFIRM_LOCATION + ":" + task.getId()));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttons, 1));
        return inlineKeyboardMarkup;
    }

    private String getTaskInfo(QuestGame game, Task task, int num, int count) {
        return "Ваши баллы: " + game.getPoints() +
                "\nВаши штрафы: " + game.getPenalties() +
                "\n\nДоступные задания\n" +
                num + "/" + count +
                "\n\nЗадание: \"" + task.getText() + "\"";
    }

    private String getTaskInfo(QuestGame game, Task task) {
        return "Ваши баллы: " + game.getPoints() +
                "\nВаши штрафы: " + game.getPenalties() +
                "\n\nЗадание: \"" + task.getText() + "\"";
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

    private SendPhoto getMessageToCheckPhoto(Photo photo, String message, long objectId, String objectType, long userId, long adminId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(adminId));
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photo.getBytes()), photo.getName()));
        sendPhoto.setCaption(message);

        List<InlineButtonDTO> buttons = new ArrayList<>();
        buttons.add(new InlineButtonDTO(CONFIRM_PHOTO, CONFIRM_PHOTO + ":" + objectId + objectType + USER + ":" + userId));
        buttons.add(new InlineButtonDTO(NOT_CONFIRM_PHOTO, NOT_CONFIRM_PHOTO + ":" + objectId + objectType + USER + ":" + userId));
        sendPhoto.setReplyMarkup(ButtonsUtil.getInlineButtons(buttons, 2));
        return sendPhoto;
    }

    public static String getQuestGameResultMessage(QuestGame questGame, Quest quest) {
        Duration duration = Duration.between(questGame.getStartTime(), questGame.getEndTime());
        Duration result = duration.minusMinutes(questGame.getPoints()).plusMinutes(questGame.getPenalties());
        return "Квест \"" + ReservedCharacters.replace(quest.getName()) + "\" завершён\\! " +
                "\n\nВремя выполнения: " + ReservedCharacters.replace(getTimeString(duration))
                + "\nНабрано очков: " + questGame.getPoints()
                + "\nНабрано штрафов: " + questGame.getPenalties()
                + "\n*Результат: " + ReservedCharacters.replace(getTimeString(result)) + "*";
    }

    private static String getTimeString(Duration duration) {
        return duration.toHoursPart() + " ч  "
                + duration.toMinutesPart() + " мин  "
                + duration.toSecondsPart() + " сек";
    }
}
