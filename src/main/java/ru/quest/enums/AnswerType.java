package ru.quest.enums;

public enum AnswerType {
    TEXT("Текст"), PHOTO("Фото");

    private String type;

    AnswerType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static AnswerType getByTypeName(String typeName) {
        return switch (typeName) {
            case "Текст" -> AnswerType.TEXT;
            case "Фото" -> AnswerType.PHOTO;
            default -> null;
        };
    }
}
