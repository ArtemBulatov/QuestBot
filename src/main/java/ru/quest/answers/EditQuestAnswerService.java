package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.QuestType;
import ru.quest.models.Quest;
import ru.quest.models.Task;
import ru.quest.services.HintService;
import ru.quest.services.LastAnswerService;
import ru.quest.services.QuestService;
import ru.quest.services.TaskService;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.ReservedCharacters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.quest.answers.AnswerConstants.*;
import static ru.quest.answers.AnswerConstants.CHANGE_INDEX;
import static ru.quest.answers.EditTaskAnswerService.*;

@Service
public class EditQuestAnswerService implements AnswerService{
    public static final String CREATE_NEW_QUEST = "Создать новый квест";
    private static final String DELETE_QUEST= "Удалить квест";
    private static final String ENTER_NAME = "Введите название";
    private static final String CHOOSE_TYPE = "Выберите тип квеста";
    private static final String ENTER_DESCRIPTION = "Введите описание";
    private static final String ENTER_DATETIME = "Введите дату и время начала квеста в формате 12.12.2012 12:00";
    public static final String THIS_QUEST = "thisQuest";
    private static final String ADD_NEW_INSTRUCTION = "Добавить инструкцию";
    private static final String ENTER_INSTRUCTION_TEXT = "Добавить инструкцию";
    private static final String SHOW_INSTRUCTION = "Посмотреть инструкцию";
    private static final String DELETE_INSTRUCTION = "Удалить инструкцию";
    private static final String CHANGE_DATETIME = "Изменить дату и время";

    private final QuestService questService;
    private final TaskService taskService;
    private final HintService hintService;
    private final LastAnswerService lastAnswerService;

    private final Map<Long, Quest> bufferForNewQuests = new HashMap<>();

    public EditQuestAnswerService(QuestService questService, TaskService taskService, HintService hintService, LastAnswerService lastAnswerService) {
        this.questService = questService;
        this.taskService = taskService;
        this.hintService = hintService;
        this.lastAnswerService = lastAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if(dto.getText().equals("/start")) {
            answerDTO.getMessages().add(getSendMessage("Для работы с ботом используйте меню", dto.getChatId()));
        }
        else if (dto.getText().equals("/quests")) {
            bufferForNewQuests.remove(dto.getChatId());
            List<Quest> quests = questService.getaAll();
            if (quests.isEmpty()) {
                String[] buttons = new String[1];
                buttons[0] = CREATE_NEW_QUEST;
                answerDTO.getMessages().add(getSendMessage("Нет созданных квестов", buttons, dto.getChatId()));
                return answerDTO;
            }
            sendMessageForQuest(quests, 0, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(THIS_QUEST + ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] parts = dto.getText().split(" ", 2);
            long questId = Long.parseLong(parts[0].split(":", 2)[1]);
            int change = Integer.parseInt(parts[1].split(":", 2)[1]);

            List<Quest> quests = questService.getaAll();
            int index = quests.indexOf(quests.stream().filter(thisQuest -> thisQuest.getId() == questId).findFirst().get());

            if(index+change < 0) {
                index = quests.size()-1;
            }
            else if (index+change > quests.size()-1) {
                index = 0;
            }
            else {
                index = index+change;
            }
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(DELETE_QUEST + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":", 2)[1]);
            List<Quest> quests = questService.getaAll();
            Quest questToDelete = quests.stream().filter(quest -> quest.getId() == questId).findFirst().get();
            int index = quests.indexOf(questToDelete);

            quests.remove(questToDelete);
            questService.delete(questToDelete);

            if (quests.isEmpty()) {
                String[] buttons = new String[1];
                buttons[0] = CREATE_NEW_QUEST;
                answerDTO.getMessages().add(getSendMessage("Нет созданных квестов", buttons, dto.getChatId()));
                return answerDTO;
            }

            index = Math.max(index - 1, 0);
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().equals(CREATE_NEW_QUEST)) {
            Quest quest = new Quest();
            bufferForNewQuests.put(dto.getChatId(), quest);
            String[] buttons = new String[2];
            buttons[0] = QuestType.INDIVIDUAL.getType();
            buttons[1] = QuestType.GROUP.getType();
            answerDTO.getMessages().add(getSendMessage(CHOOSE_TYPE, buttons, dto.getChatId()));
            lastAnswerService.addLastAnswer(CHOOSE_TYPE, dto.getChatId());
        }
        else if ((dto.getText().equals(QuestType.INDIVIDUAL.getType()) || dto.getText().equals(QuestType.GROUP.getType()))
                && lastAnswerService.readLastAnswer(dto.getChatId()).equals(CHOOSE_TYPE)
                && bufferForNewQuests.containsKey(dto.getChatId())) {
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            bufferForNewQuests.get(dto.getChatId()).setType(QuestType.getByTypeName(dto.getText()));
            answerDTO.getMessages().add(getSendMessage(ENTER_NAME, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_NAME, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_NAME)
                && bufferForNewQuests.containsKey(dto.getChatId())) {
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            bufferForNewQuests.get(dto.getChatId()).setName(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_DESCRIPTION, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_DESCRIPTION, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_DESCRIPTION)
                && bufferForNewQuests.containsKey(dto.getChatId())) {
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            bufferForNewQuests.get(dto.getChatId()).setDescription(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_DATETIME, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_DATETIME, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_DATETIME)
                && bufferForNewQuests.containsKey(dto.getChatId())) {
            if (!dto.getText().matches("\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}")) {
                answerDTO.getMessages().add(getSendMessage("Вы ввели дату и время в неправильном формате", dto.getChatId()));
                return answerDTO;
            }
            Quest quest = bufferForNewQuests.remove(dto.getChatId());
            quest.setDateTime(LocalDateTime.parse(dto.getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            String message = "Квест сохранён";
            if (quest.getId() != 0) {
                message = "Дата и время квеста изменены";
            }
            long questId = questService.save(quest).getId();
            answerDTO.getMessages().add(getSendMessage(message, dto.getChatId()));

            List<Quest> quests = questService.getaAll();
            int index = quests.indexOf(quests.stream().filter(thisQuest -> thisQuest.getId() == questId).findFirst().get());
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(ADD_NEW_INSTRUCTION + ":\\d+")) {
            answerDTO.getMessages().add(getSendMessage(ENTER_INSTRUCTION_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(dto.getText(), dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ADD_NEW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setInstruction(dto.getText());
            questService.save(quest);

            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_INSTRUCTION, DELETE_INSTRUCTION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getMessages().add(getSendMessageWithInlineButtons(getQuestInstruction(quest), buttons, 1, true, dto.getChatId()));
        }
        else if (dto.getText().matches(SHOW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_INSTRUCTION, DELETE_INSTRUCTION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getEditMessages().add(getEditMessageText(getQuestInstruction(quest), buttons, 1, true, dto.getChatId(), dto.getMessageId()));
        }
        else if (dto.getText().matches(DELETE_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setInstruction("");
            questService.save(quest);

            List<Quest> quests = questService.getaAll();
            int index = quests.indexOf(quests.stream().filter(thisQuest -> thisQuest.getId() == questId).findFirst().get());
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(CHANGE_DATETIME + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            bufferForNewQuests.put(dto.getChatId(), quest);
            answerDTO.getMessages().add(getSendMessage(ENTER_DATETIME, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_DATETIME, dto.getChatId());
        }
        return answerDTO;
    }

    private void sendMessageForQuest(List<Quest> quests, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Quest quest = quests.get(index);
        if (messageId == 0) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(getQuestInfo(quest, index+1, quests.size()));
            sendMessage.setParseMode("MarkdownV2");
            sendMessage.setReplyMarkup(getInlineKeyboardMarkup(quest));

            answerDTO.getMessages().add(sendMessage);
            return;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setMessageId(messageId);
        editMessageText.setText(getQuestInfo(quest, index+1, quests.size()));
        editMessageText.setParseMode("MarkdownV2");
        editMessageText.setReplyMarkup(getInlineKeyboardMarkup(quest));

        answerDTO.getEditMessages().add(editMessageText);
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Quest quest) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        List<Task> tasks = taskService.getAllByQuestId(quest.getId());
        if (tasks.isEmpty()) {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_TASK, ADD_NEW_TASK + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(SHOW_TASKS, EditTaskAnswerService.getButtonDataToShowTask(quest.getId(), tasks.get(0).getId(), 0)));
        }
        if (quest.getInstruction() != null && !quest.getInstruction().isEmpty()) {
         buttonDTOList.add(new InlineButtonDTO(SHOW_INSTRUCTION, SHOW_INSTRUCTION + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_INSTRUCTION, ADD_NEW_INSTRUCTION + ":" + quest.getId()));
        }
        buttonDTOList.add(new InlineButtonDTO(CHANGE_DATETIME, CHANGE_DATETIME + ":" + quest.getId()));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowQuest(quest.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowQuest(quest.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(DELETE_QUEST, DELETE_QUEST + ":" + quest.getId()));
        buttonDTOList.add(new InlineButtonDTO(CREATE_NEW_QUEST, CREATE_NEW_QUEST));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }

    public static String getButtonDataToShowQuest(long questId, int index) {
        return THIS_QUEST + ":" + questId + " " + CHANGE_INDEX + ":" + index;
    }

    private String getQuestInfo(Quest quest, int num, int count) {
        return num + "/" + count +
                "\n\nНазвание квеста: *" + quest.getName() + "*" +
                "\n\nДата и время начала: " + ReservedCharacters.replace(quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) +
                "\n\nОписание квеста: \n" + ReservedCharacters.replace(quest.getDescription());
    }

    private String getQuestInstruction(Quest quest) {
        return "\n\nНазвание квеста: *" + quest.getName() + "*" +
                "\n\nИнструкция: \n" + ReservedCharacters.replace(quest.getInstruction());
    }
}
