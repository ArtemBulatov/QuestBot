package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Service
public class LastAnswerService {
    private final Map<Long, LinkedList<String>> lastAnswerMap;
    private final UserService usersService;

    public LastAnswerService(UserService usersService) {
        this.usersService = usersService;
        this.lastAnswerMap = new HashMap<>();
        initMaps();
    }

    public void addLastAnswer(String message, long chatId) {
        if(!lastAnswerMap.containsKey(chatId)) {
            lastAnswerMap.put(chatId, new LinkedList<>());
        }
        lastAnswerMap.get(chatId).add(message);
    }

    public String readLastAnswer(long chatId) {
        if (lastAnswerMap.get(chatId) == null || lastAnswerMap.get(chatId).isEmpty()) {
            return "";
        }
        checkSizeOfAnswerList(chatId);
        return lastAnswerMap.get(chatId).getLast();
    }

    public String deleteLastAnswer(long chatId) {
        if (lastAnswerMap.get(chatId).isEmpty()) {
            return "";
        }
        return lastAnswerMap.get(chatId).removeLast();
    }

    public boolean answersIsEmpty(long chatId) {
        return lastAnswerMap.get(chatId).isEmpty();
    }

    public int getSizeOfAnswers(long chatId) {
        return lastAnswerMap.get(chatId).size();
    }

    public LinkedList<String> getAnswerList(long chatId) {
        return lastAnswerMap.get(chatId);
    }

    public void createAnswerList(long chatId) {
        lastAnswerMap.put(chatId, new LinkedList<>());
    }

    public void clearAnswers(long chatId) {
        lastAnswerMap.put(chatId, new LinkedList<>());
    }

    private void checkSizeOfAnswerList(long userChatId) {
        if (lastAnswerMap.get(userChatId).size() > 15) {
            for (int i = 0; i < 5; i++) {
                lastAnswerMap.get(userChatId).removeFirst();
            }
        }
    }
    private void initMaps() {
        usersService.getAll().forEach(user -> {
            lastAnswerMap.put(user.getId(), new LinkedList<>());
        });
    }

}
