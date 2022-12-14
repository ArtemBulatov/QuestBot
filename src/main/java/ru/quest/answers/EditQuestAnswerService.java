package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.QuestType;
import ru.quest.models.Quest;
import ru.quest.models.Task;
import ru.quest.services.*;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.ReservedCharacters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ru.quest.answers.constants.AnswerConstants.*;
import static ru.quest.answers.EditTaskAnswerService.*;
import static ru.quest.answers.constants.EpilogueConstants.ADD_NEW_EPILOGUE;
import static ru.quest.answers.constants.EpilogueConstants.SHOW_EPILOGUE;
import static ru.quest.answers.constants.InstructionConstants.*;
import static ru.quest.answers.constants.PrologueConstants.ADD_NEW_PROLOGUE;
import static ru.quest.answers.constants.PrologueConstants.SHOW_PROLOGUE;

@Service
public class EditQuestAnswerService implements AnswerService {
    public static final String CREATE_NEW_QUEST = "Создать новый квест";

    private static final String DELETE_QUEST= "Удалить квест";
    private static final String ASK_ABOUT_DELETE_QUEST= "Вы действительно хотите безвозвратно удалить квест";
    private static final String ENTER_NAME = "Введите название";
    private static final String CHOOSE_TYPE = "Выберите тип квеста";
    private static final String ENTER_DESCRIPTION = "Введите описание";
    private static final String ENTER_DATETIME = "Введите дату и время начала квеста в формате 12.12.2012 12:00";
    private static final String CHANGE_DATETIME = "Изменить дату и время";
    private static final String ADD_NEW_END_NOTIFICATION = "Добавить уведомление за 30 мин";
    private static final String ENTER_END_NOTIFICATION_TEXT = "Введите текст уведомления за 30 мин";
    private static final String SHOW_END_NOTIFICATION = "Посмотреть уведомление за 30 мин";
    private static final String DELETE_END_NOTIFICATION = "Удалить уведомление за 30 мин";


    private final QuestService questService;
    private final TaskService taskService;
    private final LastAnswerService lastAnswerService;
    private final PrologueService prologueService;
    private final EpilogueService epilogueService;
    private final InstructionService instructionService;

    private final Map<Long, Quest> bufferForNewQuests = new HashMap<>();

    public EditQuestAnswerService(QuestService questService,
                                  TaskService taskService,
                                  LastAnswerService lastAnswerService,
                                  PrologueService prologueService,
                                  EpilogueService epilogueService, InstructionService instructionService) {
        this.questService = questService;
        this.taskService = taskService;
        this.lastAnswerService = lastAnswerService;
        this.prologueService = prologueService;
        this.epilogueService = epilogueService;
        this.instructionService = instructionService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();

        if(dto.getText().contains("/start")) {
            answerDTO.getMessages().add(getSendMessage("Для работы с ботом используйте меню", dto.getChatId()));
        }
        else if (dto.getText().equals("/quests")) {
            bufferForNewQuests.remove(dto.getChatId());
            bufferMapForTasks.remove(dto.getChatId());
            EditHintAnswerService.bufferMapForHints.remove(dto.getChatId());

            List<Quest> quests = questService.getaAllNotDeleted();
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

            List<Quest> quests = questService.getaAllNotDeleted();
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
            Quest quest = questService.get(questId);
            String message = ASK_ABOUT_DELETE_QUEST + " \"" + quest.getName() + "\"?";
            String[] buttons = new String[2];
            buttons[0] = YES;
            buttons[1] = NO;
            answerDTO.getMessages().add(getSendMessage(message, buttons, dto.getChatId()));
            lastAnswerService.addLastAnswer(ASK_ABOUT_DELETE_QUEST + ":" + questId, dto.getChatId());
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
        else if (dto.getText().matches(ADD_NEW_INSTRUCTION + ":\\d+")) {
            answerDTO.getMessages().add(getSendMessage(ENTER_INSTRUCTION_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(dto.getText(), dto.getChatId());
        }
        else if (dto.getText().matches(SHOW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_INSTRUCTION, DELETE_INSTRUCTION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getEditMessages().add(getEditMessageText(getQuestInstruction(quest), buttons, 1, true, dto.getChatId(), dto.getMessageId()));
        }
        else if (dto.getText().matches(DELETE_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setInstruction("");
            questService.save(quest);

            List<Quest> quests = questService.getaAllNotDeleted();
            int index = quests.indexOf(quests.stream().filter(thisQuest -> thisQuest.getId() == questId).findFirst().get());
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(ADD_NEW_END_NOTIFICATION + ":\\d+")) {
            answerDTO.getMessages().add(getSendMessage(ENTER_END_NOTIFICATION_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(dto.getText(), dto.getChatId());
        }
        else if (dto.getText().matches(SHOW_END_NOTIFICATION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_END_NOTIFICATION, DELETE_END_NOTIFICATION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getEditMessages().add(getEditMessageText(getQuestEndNotification(quest), buttons, 1, true, dto.getChatId(), dto.getMessageId()));
        }
        else if (dto.getText().matches(DELETE_END_NOTIFICATION + ":\\d+")) {
            long questId = Long.parseLong(dto.getText().split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setNotificationBeforeEnd("");
            questService.save(quest);

            List<Quest> quests = questService.getaAllNotDeleted();
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


        else if (lastAnswerService.readLastAnswer(dto.getChatId()).contains(ASK_ABOUT_DELETE_QUEST) && dto.getText().equals(YES)) {
            long questId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            Quest thisQuest = questService.get(questId);
            List<Quest> quests = questService.getaAllNotDeleted();
            Quest questToDelete = quests.stream().filter(quest -> quest.getId() == thisQuest.getId()).findFirst().get();
            int index = quests.indexOf(questToDelete);

            String questName = thisQuest.getName();
            quests.remove(questToDelete);
            questService.delete(questToDelete);

            answerDTO.getMessages().add(getSendMessage("Квест \"" + questName + "\" удалён", dto.getChatId()));

            if (quests.isEmpty()) {
                String[] buttons = new String[1];
                buttons[0] = CREATE_NEW_QUEST;
                answerDTO.getMessages().add(getSendMessage("Нет созданных квестов", buttons, dto.getChatId()));
                return answerDTO;
            }

            index = Math.max(index - 1, 0);
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).contains(ASK_ABOUT_DELETE_QUEST) && dto.getText().equals(NO)) {
            long questId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            List<Quest> quests = questService.getaAllNotDeleted();
            Quest thisQuest = quests.stream().filter(quest -> quest.getId() == questId).findFirst().get();
            int index = quests.indexOf(thisQuest);

            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
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

            List<Quest> quests = questService.getaAllNotDeleted();
            int index = quests.indexOf(quests.stream().filter(thisQuest -> thisQuest.getId() == questId).findFirst().get());
            sendMessageForQuest(quests, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ADD_NEW_INSTRUCTION + ":\\d+")) {
            long questId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setInstruction(dto.getText());
            questService.save(quest);

            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_INSTRUCTION, DELETE_INSTRUCTION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getMessages().add(getSendMessageWithInlineButtons(getQuestInstruction(quest), buttons, 1, true, dto.getChatId()));
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ADD_NEW_END_NOTIFICATION + ":\\d+")) {
            long questId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":")[1]);
            Quest quest = questService.get(questId);
            quest.setNotificationBeforeEnd(dto.getText());
            questService.save(quest);

            List<InlineButtonDTO> buttons = new ArrayList<>();
            buttons.add(new InlineButtonDTO(DELETE_END_NOTIFICATION, DELETE_END_NOTIFICATION + ":" + quest.getId()));
            buttons.add(new InlineButtonDTO(BACK, Quest.getButtonDataToShowQuest(quest.getId(), 0)));
            answerDTO.getMessages().add(getSendMessageWithInlineButtons(getQuestEndNotification(quest), buttons, 1, true, dto.getChatId()));
        }

        return answerDTO;
    }

    private void sendMessageForQuest(List<Quest> quests, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Quest quest = quests.get(index);
        String questInfo = getQuestInfo(quest, index+1, quests.size());

        if (messageId != 0) {
            answerDTO.getDeleteMessages().add(getDeleteMessage(chatId, messageId));
        }
        answerDTO.getMessages().add(getSendMessage(questInfo, true, getInlineKeyboardMarkup(quest), chatId));
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

        if (instructionService.findByQuestId(quest.getId()).isPresent()) {
            buttonDTOList.add(new InlineButtonDTO(SHOW_INSTRUCTION, SHOW_INSTRUCTION + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_INSTRUCTION, ADD_NEW_INSTRUCTION + ":" + quest.getId()));
        }

        if (prologueService.findByQuestId(quest.getId()) != null) {
            buttonDTOList.add(new InlineButtonDTO(SHOW_PROLOGUE, SHOW_PROLOGUE + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_PROLOGUE, ADD_NEW_PROLOGUE + ":" + quest.getId()));
        }

        if (epilogueService.findByQuestId(quest.getId()) != null) {
            buttonDTOList.add(new InlineButtonDTO(SHOW_EPILOGUE, SHOW_EPILOGUE + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_EPILOGUE, ADD_NEW_EPILOGUE + ":" + quest.getId()));
        }

        if (quest.getNotificationBeforeEnd() != null && !quest.getNotificationBeforeEnd().isEmpty()) {
            buttonDTOList.add(new InlineButtonDTO(SHOW_END_NOTIFICATION, SHOW_END_NOTIFICATION + ":" + quest.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(ADD_NEW_END_NOTIFICATION, ADD_NEW_END_NOTIFICATION + ":" + quest.getId()));
        }

        buttonDTOList.add(new InlineButtonDTO(CHANGE_DATETIME, CHANGE_DATETIME + ":" + quest.getId()));
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, Quest.getButtonDataToShowQuest(quest.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, Quest.getButtonDataToShowQuest(quest.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(DELETE_QUEST, DELETE_QUEST + ":" + quest.getId()));
        buttonDTOList.add(new InlineButtonDTO(CREATE_NEW_QUEST, CREATE_NEW_QUEST));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }

    private String getQuestInfo(Quest quest, int num, int count) {
        return num + "/" + count +
                "\n\nНазвание квеста: *" + quest.getName() + "*" +
                "\n\nДата и время начала: " + ReservedCharacters.replace(quest.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) +
                "\nТип квеста: " + quest.getType().getType() +
                "\n\nОписание квеста: \n" + ReservedCharacters.replace(quest.getDescription());
    }

    private String getQuestInstruction(Quest quest) {
        return "\n\nНазвание квеста: *" + quest.getName() + "*" +
                "\n\nИнструкция: \n" + ReservedCharacters.replace(quest.getInstruction());
    }

    private String getQuestEndNotification(Quest quest) {
        return "\n\nНазвание квеста: *" + quest.getName() + "*" +
                "\n\nУведомление за 30 мин до окончания квеста: \n\""
                + ReservedCharacters.replace(quest.getNotificationBeforeEnd()) + "\"";
    }
}
