package org.rozkladbot.DBControllers;

import org.json.simple.parser.ParseException;
import org.rozkladbot.entities.Group;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.data.AbstractJsonDeserializer;
import org.rozkladbot.utils.data.GroupUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

@Repository("GroupDB")
public class GroupDB {
    private static ConcurrentHashMap<String, Group> groups;
    private static final ConsoleLineLogger<GroupDB> log = new ConsoleLineLogger<>(GroupDB.class);
    private static final AbstractJsonDeserializer<String, Group> groupDeserializer = new GroupUtils();
    public GroupDB() {

    }

    // Каждый день в 6:00 будет происходить обновление списков групп для улучшения гибкости.
    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Kiev")
    public static void fetchGroups() {
        parseFile();
        System.out.println(groups);
    }

    private static void parseFile() {
        try {
            groups = new ConcurrentHashMap<>(groupDeserializer.deserialize(
                    "groups", "groupsList.json", "groups",
                    "institute", "group", "faculty", "groupNumber", "course"));
        } catch (IOException | ParseException exception) {
            try {
                log.info("Здається, що списку груп немає...Спробую створити.");
                Path directoryPath = Paths.get("groups");
                Path filePath = directoryPath.resolve("groupsList.json");
                if (!Files.exists(filePath)) {
                    Files.createDirectories(directoryPath);
                    Files.createFile(filePath);
                }
                log.success("Файл-список групи створено успішно!");
            } catch (IOException ioException) {
                log.error("Помилка при створенні файлу-списку груп.");
            }
        }
    }

    public static ConcurrentHashMap<String, Group> getGroups() {
        return groups;
    }
}
