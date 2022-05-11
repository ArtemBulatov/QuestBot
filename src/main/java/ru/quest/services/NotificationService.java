package ru.quest.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.quest.QuestBot;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.enums.RegistrationStatus;
import ru.quest.models.Quest;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.KhantyMansiyskDateTime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.quest.answers.QuestAnswerService.GET_TASKS;

@Service
public class NotificationService {
    private final QuestService questService;
    private final RegistrationService registrationService;
    private final QuestBot questBot;

    public NotificationService(QuestService questService, RegistrationService registrationService, QuestBot questBot) {
        this.questService = questService;
        this.registrationService = registrationService;
        this.questBot = questBot;
    }

    @Scheduled(fixedRate = 60000)
    private void checkNotifications() {
        LocalDateTime dateTime = KhantyMansiyskDateTime.now();
        List<Quest> quests = questService.getaAll();
        if (quests.isEmpty()) {
            return;
        }

        quests.stream().filter(quest -> quest.getDateTime().isAfter(dateTime))
                .forEach(quest -> {
                    Duration duration = Duration.between(dateTime, quest.getDateTime());
                    long minutes = duration.toMinutes();
                    if (minutes == 0 || minutes == 60 || minutes == 60*24) {
                        try {
                            Thread.sleep(duration.toSecondsPart() * 1000L);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (minutes == 0) {
                        registrationService.getAllByQuestId(quest.getId())
                                .stream().filter(registration -> registration.isConfirmed() && registration.getStatus() == RegistrationStatus.APPROVED)
                                .forEach(registration -> {
                                    SendMessage sendMessage = getSendMessage("Квест \"" + quest.getName() + "\" начинается!",
                                            registration.getUserId());

                                    List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                                    buttonDTOList.add(new InlineButtonDTO(GET_TASKS, GET_TASKS + ":" + quest.getId()));
                                    sendMessage.setReplyMarkup(ButtonsUtil.getInlineButtons(buttonDTOList, 1));
                                    questBot.sendTheMessage(sendMessage);
                                });
                    }
                    if (minutes == 60) {
                        registrationService.getAllByQuestId(quest.getId())
                                .stream().filter(registration -> registration.isConfirmed() && registration.getStatus() == RegistrationStatus.APPROVED)
                                .forEach(registration -> {
                                    String instructionText = "";
                                    if (quest.getInstruction() != null && !quest.getInstruction().isEmpty()) {
                                        instructionText = "\n\nИнструкция для прохождения квеста:\n" + quest.getInstruction();
                                    }
                                    questBot.sendTheMessage(getSendMessage("❗Напоминание.\n" +
                                            "Уже через час начинается квест \"" + quest.getName() + "\"" + instructionText, registration.getUserId()));
                                });
                    }
                    if (minutes == 60*24) {
                        registrationService.getAllByQuestId(quest.getId())
                                .stream().filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                                .forEach(registration -> {
                                    String text = "Напоминание.\n" +
                                            "Завтра в " + quest.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                                            + " начинается квест \"" + quest.getName() + "\""
                                            + "\nВы подтверждаете своё участие?";

                                    SendMessage sendMessage = getSendMessage(text, registration.getUserId());
                                    List<InlineButtonDTO> buttonDTOList = new ArrayList<>();
                                    buttonDTOList.add(new InlineButtonDTO("Подтвердить", "confirm:" + quest.getId()));
                                    buttonDTOList.add(new InlineButtonDTO("Отказаться", "reject:" + quest.getId()));
                                    sendMessage.setReplyMarkup(ButtonsUtil.getInlineButtons(buttonDTOList,1));
                                    questBot.sendTheMessage(sendMessage);
                                });
                    }
        });
    }

    private SendMessage getSendMessage(String text, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        return sendMessage;
    }
}
