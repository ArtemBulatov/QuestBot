package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.quest.QuestBot;
import ru.quest.answers.QuestAnswerService;
import ru.quest.dto.InlineButtonDTO;
import ru.quest.enums.RegistrationStatus;
import ru.quest.models.Instruction;
import ru.quest.models.Prologue;
import ru.quest.models.Quest;
import ru.quest.utils.ButtonsUtil;
import ru.quest.utils.KhantyMansiyskDateTime;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.quest.answers.QuestAnswerService.BEGIN_QUEST;
import static ru.quest.answers.QuestAnswerService.GET_TASKS;

@Slf4j
@Service
public class NotificationService {
    private final QuestService questService;
    private final PrologueService prologueService;
    private final InstructionService instructionService;
    private final RegistrationService registrationService;
    private final QuestGameService questGameService;
    private final QuestBot questBot;

    public NotificationService(QuestService questService,
                               PrologueService prologueService,
                               InstructionService instructionService,
                               RegistrationService registrationService,
                               QuestGameService questGameService,
                               QuestBot questBot) {
        this.questService = questService;
        this.prologueService = prologueService;
        this.instructionService = instructionService;
        this.registrationService = registrationService;
        this.questGameService = questGameService;
        this.questBot = questBot;
    }

    @Scheduled(fixedRate = 60000)
    private void checkNotifications() {
        LocalDateTime dateTime = KhantyMansiyskDateTime.now();
        List<Quest> quests = questService.getaAllNotDeleted();
        if (quests.isEmpty()) {
            return;
        }

        try {
            Thread.sleep((60 - dateTime.getSecond()) * 1000L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        quests.stream().filter(quest -> quest.getDateTime().isAfter(dateTime))
                .forEach(quest -> {
                    Duration duration = Duration.between(dateTime, quest.getDateTime());
                    long minutes = duration.toMinutes();
                    if (minutes == 0) {
                        registrationService.getAllByQuestId(quest.getId())
                                .stream().filter(registration -> registration.isConfirmed() && registration.getStatus() == RegistrationStatus.APPROVED)
                                .forEach(registration -> {
                                    SendMessage sendMessage = getSendMessage("Квест \"" + quest.getName() + "\" начинается!",
                                            registration.getUserId());

                                    List<InlineButtonDTO> buttonDTOList = new ArrayList<>();

                                    Prologue prologue = prologueService.findByQuestId(quest.getId());
                                    if (prologue == null) {
                                        buttonDTOList.add(new InlineButtonDTO(GET_TASKS, GET_TASKS + ":" + quest.getId()));
                                    }
                                    else {
                                        buttonDTOList.add(new InlineButtonDTO(BEGIN_QUEST, BEGIN_QUEST + ":" + quest.getId()));
                                    }
                                    sendMessage.setReplyMarkup(ButtonsUtil.getInlineButtons(buttonDTOList, 1));
                                    questBot.sendTheMessage(sendMessage);
                                    registrationService.delete(registration.getId());
                                });
                    }
                    if (minutes == 60) {
                        registrationService.getAllByQuestId(quest.getId())
                                .stream().filter(registration -> registration.isConfirmed() && registration.getStatus() == RegistrationStatus.APPROVED)
                                .forEach(registration -> {
                                    Optional<Instruction> instructionOpt = instructionService.findByQuestId(registration.getQuestId());
                                    String notificationMessage = "❗Напоминание.\n" +
                                            "Уже через час начинается квест \"" + quest.getName() + "\"";
                                    if (instructionOpt.isPresent()) {
                                        Instruction instruction = instructionOpt.get();
                                        String instructionMessage = notificationMessage +
                                                "\n\nИнструкция для прохождения квеста:\n" + instruction.getText();
                                        if (instruction.getVideo() == null) {
                                            questBot.sendTheMessage(getSendMessage(instructionMessage, registration.getUserId()));
                                        }
                                        else {
                                            InputFile videoFile = new InputFile(new ByteArrayInputStream(instruction.getVideo().getBytes()), instruction.getVideo().getName());
                                            questBot.sendTheVideo(getSendVideo(instructionMessage, videoFile, registration.getUserId()));
                                        }
                                    }
                                    else {
                                        questBot.sendTheMessage(getSendMessage(notificationMessage, registration.getUserId()));
                                    }
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

        questGameService.getActiveQuestGames().forEach(questGame -> {
            Quest quest = questService.get(questGame.getQuestId());
            LocalDateTime dateTimeAfterFourHours = quest.getDateTime().plusHours(4);
            Duration durationToEndQuest = Duration.between(dateTime, dateTimeAfterFourHours);
            long minutes = durationToEndQuest.toMinutes();
            if (minutes == 30 && quest.getNotificationBeforeEnd() != null) {
                questBot.sendTheMessage(getSendMessage(quest.getNotificationBeforeEnd(), questGame.getUserId()));
            }
            else if (minutes == 0 || dateTime.isAfter(dateTimeAfterFourHours)) {
                questGame.setEndTime(KhantyMansiyskDateTime.now());
                questGame.setOver(true);
                questGameService.save(questGame);
                questBot.sendTheMessage(getSendMessage("Время отведённое на квест истекло!", questGame.getUserId()));
                questBot.sendTheMessage(getSendMessage(QuestAnswerService.getQuestGameResultMessage(questGame, quest),
                        true, questGame.getUserId()));
            }
        });

    }

    private SendMessage getSendMessage(String text, long chatId) {
        return getSendMessage(text, false, chatId);
    }

    private SendMessage getSendMessage(String text, boolean markdown, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        sendMessage.enableMarkdownV2(markdown);
        return sendMessage;
    }

    private SendVideo getSendVideo(String text, InputFile inputFile, long chatId) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(String.valueOf(chatId));
        sendVideo.setVideo(inputFile);
        sendVideo.setCaption(text);
        return sendVideo;
    }
}
