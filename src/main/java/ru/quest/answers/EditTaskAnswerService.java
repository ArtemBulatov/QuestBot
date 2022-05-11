package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestAdminBot;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.AdminStatus;
import ru.quest.models.Hint;
import ru.quest.models.Location;
import ru.quest.models.Quest;
import ru.quest.models.Task;
import ru.quest.services.*;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.ReservedCharacters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.quest.answers.AnswerConstants.*;
import static ru.quest.answers.EditHintAnswerService.ADD_NEW_HINT;
import static ru.quest.answers.EditHintAnswerService.SHOW_HINTS;
import static ru.quest.answers.EditQuestAnswerService.THIS_QUEST;

@Service
public class EditTaskAnswerService implements AnswerService{

    public static final String ADD_NEW_TASK = "Добавить задание";
    public static final String SHOW_TASKS = "Посмотреть задания";
    private static final String ENTER_TEXT = "Введите текст задания";
    public static final String ADD_NEW_LOCATION = "Добавить местоположение";
    public static final String SHOW_LOCATION = "Посмотреть местоположение";
    public static final String DELETE_LOCATION = "Удалить местоположение";

    private static final String LAST_TASK = "Это последнее задание";
    private static final String MARK_AS_LAST = "Отметить как последнее";
    private static final String DELETE_TASK = "Удалить задание";
    private static final String ENTER_ANSWER = "Введите ответ";

    private static final String DELETE_MESSAGE = "DELETEMESSAGE";

    public static final String QUEST_ID = "questId";
    public static final String THIS_TASK = "thisTask";

    private final TaskService taskService;
    private final QuestService questService;
    private final HintService hintService;
    private final LocationService locationService;
    private final EditQuestAnswerService editQuestAnswerService;
    private final LastAnswerService lastAnswerService;

    private final Map<Long, Task> bufferMapForTasks = new HashMap<>();

    public EditTaskAnswerService(TaskService taskService, QuestService questService, HintService hintService, LocationService locationService, EditQuestAnswerService editQuestAnswerService, LastAnswerService lastAnswerService) {
        this.taskService = taskService;
        this.questService = questService;
        this.hintService = hintService;
        this.locationService = locationService;
        this.editQuestAnswerService = editQuestAnswerService;
        this.lastAnswerService = lastAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();
        if (dto.getText().matches(ADD_NEW_TASK + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task task = new Task();
            task.setQuestId(questId);
            bufferMapForTasks.put(dto.getChatId(), task);
            answerDTO.getMessages().add(getSendMessage(ENTER_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_TEXT, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEXT)
                && bufferMapForTasks.containsKey(dto.getChatId())) {
            bufferMapForTasks.get(dto.getChatId()).setText(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_ANSWER, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_ANSWER, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_ANSWER)
                && bufferMapForTasks.containsKey(dto.getChatId())) {
            Task task = bufferMapForTasks.remove(dto.getChatId());
            task.setAnswer(dto.getText());
            Task savedTask = taskService.save(task);

            List<Task> tasks = taskService.getAllByQuestId(savedTask.getQuestId());
            task = tasks.stream().filter(thisTask -> thisTask.getId() == savedTask.getId()).findFirst().get();
            int index = tasks.indexOf(task);

            sendMessageForTask(tasks, index, dto.getChatId(), dto.getMessageId(), answerDTO);
            lastAnswerService.deleteLastAnswer(dto.getChatId());
        }
        else if (dto.getText().matches(QUEST_ID + ":\\d+ " + THIS_TASK + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] params = dto.getText().split(" ", 3);
            long questId = Long.parseLong(params[0].split(":")[1]);
            long taskId = Long.parseLong(params[1].split(":")[1]);
            int change = Integer.parseInt(params[2].split(":")[1]);

            List<Task> tasks = taskService.getAllByQuestId(questId);

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
        else if (dto.getText().matches(DELETE_TASK + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task taskToDelete = taskService.get(taskId);
            List<Task> tasks = taskService.getAllByQuestId(taskToDelete.getQuestId());
            taskToDelete = tasks.stream().filter(task -> task.getId() == taskId).findFirst().get();
            int index = tasks.indexOf(taskToDelete);

            tasks.remove(taskToDelete);
            taskService.delete(taskToDelete);

            if (tasks.isEmpty()) {
                dto.setText(THIS_QUEST + ":" + taskToDelete.getQuestId() + " " + CHANGE_INDEX + ":" + 0);
                answerDTO = editQuestAnswerService.getAnswer(dto);
                QuestAdminBot.statusMap.put(dto.getChatId(), AdminStatus.EDIT_QUEST);
                return answerDTO;
            }

            index = Math.max(index - 1, 0);
            sendMessageForTask(tasks, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if(dto.getText().matches(ADD_NEW_LOCATION + ":\\d+")) {
            SendMessage sendMessage = getSendMessage("Отправьте местоположение", dto.getChatId());
            sendMessage.setReplyMarkup(ButtonsUtil.getLocationButton("Отправить местоположение"));
            answerDTO.getMessages().add(sendMessage);
            lastAnswerService.addLastAnswer(dto.getText(), dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ADD_NEW_LOCATION + ":\\d+") && dto.getLocation() != null) {
            long taskId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);
            Task task = taskService.get(taskId);
            Location location = locationService.save(dto.getLocation());
            task.setLocation(location);
            taskService.save(task);
            answerDTO.getMessages().add(getSendMessage("Местоположение задания сохранено", dto.getChatId()));
            sendTaskLocation(task, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(SHOW_LOCATION + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task task = taskService.get(taskId);
            answerDTO.getDeleteMessages().add(getDeleteMessage(dto.getChatId(), dto.getMessageId()));
            sendTaskLocation(task, dto.getChatId(), answerDTO);
        }
        else if (dto.getText().matches(DELETE_MESSAGE + THIS_TASK + ":\\d+") && dto.getMessageId() != 0) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task task = taskService.get(taskId);

            List<Task> tasks = taskService.getAllByQuestId(task.getQuestId());
            Task taskForIndex = tasks.stream().filter(thisTask -> thisTask.getId() == task.getId()).findFirst().get();
            int index = tasks.indexOf(taskForIndex);

            answerDTO.getDeleteMessages().add(getDeleteMessage(dto.getChatId(), dto.getMessageId()));
            sendMessageForTask(tasks, index, dto.getChatId(), 0, answerDTO);
        }
        else if (dto.getText().matches(DELETE_LOCATION + THIS_TASK + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task task = taskService.get(taskId);
            Location location = task.getLocation();
            task.setLocation(null);
            taskService.save(task);
            locationService.delete(location);

            List<Task> tasks = taskService.getAllByQuestId(task.getQuestId());
            Task taskForIndex = tasks.stream().filter(thisTask -> thisTask.getId() == task.getId()).findFirst().get();
            int index = tasks.indexOf(taskForIndex);

            answerDTO.getDeleteMessages().add(getDeleteMessage(dto.getChatId(), dto.getMessageId()));
            sendMessageForTask(tasks, index, dto.getChatId(), 0, answerDTO);
        }
        else if (dto.getText().matches(MARK_AS_LAST + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Task task = taskService.get(taskId);

            List<Task> tasks = taskService.getAllByQuestId(task.getQuestId());
            tasks.forEach(thisTask -> {
                thisTask.setLast(false);
                taskService.save(thisTask);
            });
            task.setLast(true);
            taskService.save(task);

            tasks.forEach(thisTask -> {
                if (thisTask.getId() == task.getId()) {
                    thisTask.setLast(true);
                }
            });

            Task taskForIndex = tasks.stream().filter(thisTask -> thisTask.getId() == task.getId()).findFirst().get();
            int index = tasks.indexOf(taskForIndex);

            answerDTO.getDeleteMessages().add(getDeleteMessage(dto.getChatId(), dto.getMessageId()));
            sendMessageForTask(tasks, index, dto.getChatId(), 0, answerDTO);
        }

        return answerDTO;
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

    private void editButtonsForTaskMessage(List<Task> tasks, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(String.valueOf(chatId));
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(getInlineKeyboardMarkup(tasks.get(index)));
        answerDTO.getEditMessageReplyMarkups().add(editMessageReplyMarkup);
    }

    private void sendTaskLocation(Task task, long chatId, AnswerDTO answerDTO) {
        SendLocation sendLocation = new SendLocation();
        sendLocation.setChatId(String.valueOf(chatId));
        sendLocation.setLongitude(task.getLocation().getLongitude());
        sendLocation.setLatitude(task.getLocation().getLatitude());

        List<InlineButtonDTO> buttons = new ArrayList<>();
        buttons.add(new InlineButtonDTO(DELETE_LOCATION, DELETE_LOCATION + THIS_TASK + ":" + task.getId()));
        buttons.add(new InlineButtonDTO("Вернуться к заданию", DELETE_MESSAGE + THIS_TASK + ":" + task.getId()));
        sendLocation.setReplyMarkup(ButtonsUtil.getInlineButtons(buttons, 1));
        answerDTO.getSendLocations().add(sendLocation);
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Task task) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();

        if (task.isLast()) {
            buttonDTOList.add(new InlineButtonDTO(LAST_TASK, LAST_TASK));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(MARK_AS_LAST, MARK_AS_LAST + ":" + task.getId()));
        }

        List<Hint> hints = hintService.getAllByTaskId(task.getId());
        if (hints.isEmpty()) {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_HINT, ADD_NEW_HINT + ":" + task.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(SHOW_HINTS, EditHintAnswerService.getButtonDataToShowHint(task.getId(), hints.get(0).getId(), 0)));
        }
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        if (task.getLocation() == null) {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_LOCATION, ADD_NEW_LOCATION + ":" + task.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(SHOW_LOCATION, SHOW_LOCATION + ":" + task.getId()));
        }
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowTask(task.getQuestId(), task.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowTask(task.getQuestId(), task.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(DELETE_TASK, DELETE_TASK+ ":" + task.getId()));
        buttonDTOList.add(new InlineButtonDTO(ADD_NEW_TASK, ADD_NEW_TASK + ":" + task.getQuestId()));
        buttonDTOList.add(new InlineButtonDTO(BACK, EditQuestAnswerService.getButtonDataToShowQuest(task.getQuestId(), 0)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }

    public static String getButtonDataToShowTask(long questId, long taskId, int index) {
        return QUEST_ID + ":" + questId + " " + THIS_TASK + ":" + taskId + " " + CHANGE_INDEX + ":" + index;
    }

    private String getTaskInfo(Task task, int num, int count) {
        Quest quest = questService.get(task.getQuestId());
        return "Задания квеста \"*" + quest.getName() + "*\"\n\n" +
                num + "/" + count +
                "\n\nТекст задания: \"" + ReservedCharacters.replace(task.getText()) + "\"" +
                "\n\nОтвет: \"" + ReservedCharacters.replace(task.getAnswer()) + "\"";
    }
}
