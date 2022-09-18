package ru.quest.services;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ValidationService {
    private static final Pattern phonePattern = Pattern.compile("^7[0-9]{10}$");

    public boolean isPhoneValid(String phone) {
        return phonePattern.matcher(phone).matches();
    }
}
