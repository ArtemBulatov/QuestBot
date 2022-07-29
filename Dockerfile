FROM openjdk:17-alpine3.14
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} QuestBot.jar
ENTRYPOINT ["java","-jar","QuestBot.jar"]
