package ru.quest.utils;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class KhantyMansiyskDateTime {
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Europe/Moscow")).plusHours(2);
    }
}
