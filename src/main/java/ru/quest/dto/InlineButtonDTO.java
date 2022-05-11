package ru.quest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InlineButtonDTO {
    private String name;
    private String value;
}
