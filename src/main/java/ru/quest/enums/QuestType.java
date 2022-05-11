package ru.quest.enums;

public enum QuestType {
    INDIVIDUAL("Индивидуальный"),
    GROUP("Групповой");

    private String type;

    QuestType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static QuestType getByTypeName(String typeName) {
        return switch (typeName) {
            case "Индивидуальный" -> QuestType.INDIVIDUAL;
            case "Групповой" -> QuestType.GROUP;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return type;
    }
}
