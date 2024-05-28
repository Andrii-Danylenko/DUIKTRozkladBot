package org.rozkladbot.utils;

import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.handlers.ResponseHandler;
import org.rozkladbot.interfaces.JSONWriter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
@Component("JSONWriter")
public class JSONWriterImpl implements JSONWriter {
    // Буферизация данных раз в день, которая позволяет избежать проблем с доступом к API
    @Override
    @Async
    //TODO: изменить значения
    @Scheduled(cron = "0 0 1 * * *")
    public void dumpSchedule() {
        ResponseHandler.getUsers().values().forEach(x -> {
            if (x.getGroupNumber() == null) return;
            try {
                Path directoryPath = Paths.get("groupsSchedules");
                Path filePath = directoryPath.resolve(x.getGroupNumber() + ".json");
                System.out.println(filePath);
                // Защита от повторной буферизации, если есть несколько человек из одинаковых групп
                if (!DateUtils.longToDate(filePath.toFile().lastModified()).isBefore(DateUtils.getTodayDate().minusDays(1))) {
                    System.out.printf("Файли %s вже нещодавно буферизувався%n", x.getGroupNumber() + ".json");
                    return;
                }
                HashMap<String, String> params = x.getUserParams();
                params.put("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString())));
                params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                String response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                }
                try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    System.out.printf("Завершив буферізацію файлу з росписом на цей тиждень групи %s%n%n", x.getGroupNumber());
                }
                params.replace("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(14)));
                response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                filePath = directoryPath.resolve(x.getGroupNumber() + "NextWeek.json");
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                }
                try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    System.out.printf("Завершив буферізацію файлу з росписом на наступний тиждень групи %s%n%n", x.getGroupNumber());
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public void serializeUsers() {
        // Значение пути будет стандартным (пока что?)
    }
}
