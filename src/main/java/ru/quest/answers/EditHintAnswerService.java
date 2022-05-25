package ru.quest.answers;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.quest.QuestAdminBot;
import ru.quest.dto.AnswerDTO;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.dto.MessageDTO;
import ru.quest.enums.AdminStatus;
import ru.quest.models.Hint;
import ru.quest.models.Task;
import ru.quest.services.*;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.ReservedCharacters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.quest.answers.AnswerConstants.*;

@Service
public class EditHintAnswerService implements AnswerService {
    public static final String ADD_NEW_HINT = "Добавить подсказку";
    public static final String SHOW_HINTS = "Посмотреть подсказки";
    private static final String ENTER_TEXT = "Введите текст подсказки";
    private static final String ENTER_ORDINAL_NUMBER = "Введите порядковый номер подсказки";
    private static final String DELETE_HINT= "Удалить подсказку";

    private static final String ADD_HINTS_TASK = "Добавить задание для подсказки";
    private static final String SHOW_HINTS_TASK = "Посмотреть задание для подсказки";
    private static final String ENTER_HINTS_TASK = "Введите задание для подсказки";
    private static final String DELETE_HINTS_TASK = "Удалить задание для подсказки";

    public static final String TASK_ID = "questId";
    public static final String THIS_HINT = "thisHint";

    public static final Map<Long, Hint> bufferMapForHints = new HashMap<>();

    private final HintService hintService;
    private final TaskService taskService;
    private final LastAnswerService lastAnswerService;
    private final EditTaskAnswerService editTaskAnswerService;

    public EditHintAnswerService(HintService hintService, TaskService taskService, LastAnswerService lastAnswerService, EditTaskAnswerService editTaskAnswerService) {
        this.hintService = hintService;
        this.taskService = taskService;
        this.lastAnswerService = lastAnswerService;
        this.editTaskAnswerService = editTaskAnswerService;
    }

    @Override
    public AnswerDTO getAnswer(MessageDTO dto) {
        AnswerDTO answerDTO = new AnswerDTO();
        if (dto.getText().matches(ADD_NEW_HINT + ":\\d+")) {
            long taskId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Hint hint = new Hint();
            hint.setTaskId(taskId);
            bufferMapForHints.put(dto.getChatId(), hint);
            answerDTO.getMessages().add(getSendMessage(ENTER_TEXT, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_TEXT, dto.getChatId());
        }


        else if (dto.getText().matches(TASK_ID + ":\\d+ " + THIS_HINT+ ":\\d+ " + CHANGE_INDEX + ":-?\\d+")) {
            String[] params = dto.getText().split(" ", 3);
            long taskId = Long.parseLong(params[0].split(":")[1]);
            long hintId = Long.parseLong(params[1].split(":")[1]);
            int change = Integer.parseInt(params[2].split(":")[1]);

            List<Hint> hints = hintService.getAllByTaskId(taskId);

            Hint hint = hints.stream().filter(thisHint -> thisHint.getId() == hintId).findFirst().get();
            int index = hints.indexOf(hint);

            if(index+change < 0) {
                index = hints.size()-1;
            }
            else if (index+change > hints.size()-1) {
                index = 0;
            }
            else {
                index = index+change;
            }

            sendMessageForHint(hints, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(DELETE_HINT + ":\\d+")) {
            long hintId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Hint hintToDelete = hintService.get(hintId);
            List<Hint> hints = hintService.getAllByTaskId(hintToDelete.getTaskId());
            hintToDelete = hints.stream().filter(task -> task.getId() == hintId).findFirst().get();
            int index = hints.indexOf(hintToDelete);

            hints.remove(hintToDelete);
            hintService.delete(hintToDelete);

            if (hints.isEmpty()) {
                dto.setText(getBackData(hintToDelete));
                answerDTO = editTaskAnswerService.getAnswer(dto);
                QuestAdminBot.statusMap.put(dto.getChatId(), AdminStatus.EDIT_QUEST);
                return answerDTO;
            }

            index = Math.max(index - 1, 0);
            sendMessageForHint(hints, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(ADD_HINTS_TASK+ ":\\d+")) {
            answerDTO.getMessages().add(getSendMessage(ENTER_HINTS_TASK, dto.getChatId()));
            lastAnswerService.addLastAnswer(ENTER_HINTS_TASK + ":" + dto.getText().split(":")[1], dto.getChatId());
        }

        else if (dto.getText().matches(SHOW_HINTS_TASK + ":\\d+")) {
            long hintId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Hint hint = hintService.get(hintId);
            sendMessageForHintsTask(hint, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (dto.getText().matches(DELETE_HINTS_TASK + ":\\d+")) {
            long hintId = Long.parseLong(dto.getText().split(":", 2)[1]);
            Hint hint = hintService.get(hintId);
            hint.setHintsTask(null);
            hintService.save(hint);

            List<Hint> hints = hintService.getAllByTaskId(hint.getTaskId());

            hint = hints.stream().filter(thisHint -> thisHint.getId() == hintId).findFirst().get();
            int index = hints.indexOf(hint);

            sendMessageForHint(hints, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }

        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_TEXT)
                && bufferMapForHints.containsKey(dto.getChatId())) {
            bufferMapForHints.get(dto.getChatId()).setText(dto.getText());
            answerDTO.getMessages().add(getSendMessage(ENTER_ORDINAL_NUMBER, dto.getChatId()));
            lastAnswerService.deleteLastAnswer(dto.getChatId());
            lastAnswerService.addLastAnswer(ENTER_ORDINAL_NUMBER, dto.getChatId());
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).equals(ENTER_ORDINAL_NUMBER)
                && bufferMapForHints.containsKey(dto.getChatId())) {
            int ordinalNumber;
            try {
                ordinalNumber = Integer.parseInt(dto.getText());
            } catch (NumberFormatException e) {
                answerDTO.getMessages().add(getSendMessage("Неправильный формат порядкового номера. Введите число", dto.getChatId()));
                return answerDTO;
            }
            Hint hint = bufferMapForHints.remove(dto.getChatId());
            hint.setOrdinalNumber(ordinalNumber);

            Hint savedHint = hintService.save(hint);

            List<Hint> hints = hintService.getAllByTaskId(savedHint.getTaskId());
            hint = hints.stream().filter(thisHint -> thisHint.getId() == savedHint.getId()).findFirst().get();
            int index = hints.indexOf(hint);

            sendMessageForHint(hints, index, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        else if (lastAnswerService.readLastAnswer(dto.getChatId()).matches(ENTER_HINTS_TASK + ":\\d+")) {
            long hintId = Long.parseLong(lastAnswerService.deleteLastAnswer(dto.getChatId()).split(":", 2)[1]);

            Hint hint = hintService.get(hintId);
            hint.setHintsTask(dto.getText());
            hintService.save(hint);
            sendMessageForHintsTask(hint, dto.getChatId(), dto.getMessageId(), answerDTO);
        }
        return answerDTO;
    }

    private void sendMessageForHint(List<Hint> hints, int index, long chatId, int messageId, AnswerDTO answerDTO) {
        Hint hint = hints.get(index);
        String hintInfo = getHintInfo(hint, index+1, hints.size());

        if (messageId != 0) {
            answerDTO.getDeleteMessages().add(getDeleteMessage(chatId, messageId));
        }
        answerDTO.getMessages().add(getSendMessage(hintInfo, true, getInlineKeyboardMarkup(hint), chatId));
    }

    private void sendMessageForHintsTask(Hint hint, long chatId, int messageId, AnswerDTO answerDTO) {
        List<InlineButtonDTO> buttons = new ArrayList<>();
        buttons.add(new InlineButtonDTO(DELETE_HINTS_TASK, DELETE_HINTS_TASK + ":" + hint.getId()));
        buttons.add(new InlineButtonDTO(BACK, getButtonDataToShowHint(hint.getTaskId(), hint.getId(), 0)));

        String message = "Задание для подсказки: " + hint.getHintsTask();

        if (messageId == 0) {
            answerDTO.getMessages().add(getSendMessageWithInlineButtons(message, buttons, 1, false, chatId));
        }
        else {
            answerDTO.getEditMessages().add(getEditMessageText(message, buttons, 1, false, chatId, messageId));
        }
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Hint hint) {
        InlineKeyboardMarkup inlineKeyboardMarkup =new InlineKeyboardMarkup();

        List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
        if (hint.getHintsTask() == null) {
            buttonDTOList.add(new InlineButtonDTO(ADD_HINTS_TASK, ADD_HINTS_TASK + ":" + hint.getId()));
        }
        else {
            buttonDTOList.add(new InlineButtonDTO(SHOW_HINTS_TASK, SHOW_HINTS_TASK + ":" + hint.getId()));
        }
        inlineKeyboardMarkup.setKeyboard(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(BEFORE, getButtonDataToShowHint(hint.getTaskId(), hint.getId(), -1)));
        buttonDTOList.add(new InlineButtonDTO(NEXT, getButtonDataToShowHint(hint.getTaskId(), hint.getId(), 1)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 2));

        buttonDTOList = new ArrayList<>();
        buttonDTOList.add(new InlineButtonDTO(DELETE_HINT, DELETE_HINT+ ":" + hint.getId()));
        buttonDTOList.add(new InlineButtonDTO(ADD_NEW_HINT, ADD_NEW_HINT + ":" + hint.getTaskId()));
        buttonDTOList.add(new InlineButtonDTO(BACK, getBackData(hint)));
        inlineKeyboardMarkup.getKeyboard().addAll(ButtonsUtil.getInlineButtonsRowList(buttonDTOList, 1));

        return inlineKeyboardMarkup;
    }

    private String getBackData(Hint hint) {
        Task task = taskService.get(hint.getTaskId());
        return EditTaskAnswerService.getButtonDataToShowTask(task.getQuestId(), task.getId(), 0);
    }

    public static String getButtonDataToShowHint(long taskId, long hintId, int index) {
        return TASK_ID + ":" + taskId + " " + THIS_HINT + ":" + hintId + " " + CHANGE_INDEX + ":" + index;
    }

    private String getHintInfo(Hint hint, int num, int count) {
        return num + "/" + count +
                "\n\nПорядковый номер подсказки: *" + hint.getOrdinalNumber() + "*" +
                "\n\nПодсказка: \"" + ReservedCharacters.replace(hint.getText()) + "\"";
    }
}
