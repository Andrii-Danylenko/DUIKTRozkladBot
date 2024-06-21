package org.rozkladbot.DBControllers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@Repository("GroupDB")
public class GroupDB {
    private static ConcurrentHashMap<String, Long> groups = new ConcurrentHashMap<>();
    public GroupDB() {

    }
    // Каждый день в 6:00 будет происходить обновление списков групп для улучшения гибкости.
    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Kiev")
    public static void fetchGroups() {
        parseFile();
    }
    private static void parseFile() {
        String fileName = "groups/groupsList.txt";
        try (Scanner scanner = new Scanner(new FileInputStream(fileName))) {
            while (scanner.hasNextLine()) {
                String[] keyVal = scanner.next().split("=");
                if (keyVal.length == 2) {
                    long groupId = Long.parseLong(keyVal[1]);
                    if (!groups.containsKey(keyVal[0])) {
                        groups.put(keyVal[0], groupId);
                    } else {
                        if (groups.get(keyVal[0]) != groupId) {
                            groups.replace(keyVal[0], groupId);
                        }
                    }
                }
            }
            System.out.printf("""
                    Групи, які завантажилися: %s%n""", groups);
        } catch (IOException ignored) {
            try {
                System.out.println("Здається, що списку груп немає...Спробую створити.");
                Path directoryPath = Paths.get("groups");
                Path filePath = directoryPath.resolve("groupsList.txt");
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
    public static ConcurrentHashMap<String, Long> getGroups() {
        return groups;
    }
}
