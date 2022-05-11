package ru.quest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuestBotApplication{
    public static void main(String[] args) {
        SpringApplication.run(QuestBotApplication.class);
    }
}
