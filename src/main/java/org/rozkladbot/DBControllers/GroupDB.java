package org.rozkladbot.DBControllers;

import org.json.simple.parser.ParseException;
import org.rozkladbot.entities.Group;
import org.rozkladbot.utils.FileUtils;
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

    public GroupDB() {

    }

    // Каждый день в 6:00 будет происходить обновление списков групп для улучшения гибкости.
    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Kiev")
    public static void fetchGroups() {
        parseFile();
    }

    private static void parseFile() {
        try {
            groups = new ConcurrentHashMap<>(FileUtils.deserializeGroups());
        } catch (IOException | ParseException exception) {
            try {
                System.out.println("Здається, що списку груп немає...Спробую створити.");
                Path directoryPath = Paths.get("groups");
                Path filePath = directoryPath.resolve("groupsList.json");
                if (!Files.exists(filePath)) {
                    Files.createDirectories(directoryPath);
                    Files.createFile(filePath);
                }
                System.out.println("Файл-список групи створено успішно!");
            } catch (IOException ioException) {
                System.out.println("Помилка при створенні файлу-списку груп.");
            }
        }
    }

    public static ConcurrentHashMap<String, Group> getGroups() {
        return groups;
    }
}
