package org.rozkladbot.utils.schedule;

import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.date.DateUtils;
import org.rozkladbot.web.Requester;
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
import java.util.HashSet;
import java.util.Set;

@Component("ScheduleDumper")
public class ScheduleDumper {
    private static final ConsoleLineLogger<ScheduleDumper> log = new ConsoleLineLogger<>(ScheduleDumper.class);

    @Async
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Kiev")
    public void dumpSchedule() {
        dumpSchedule(false);
    }

    @Async
    public void dumpSchedule(boolean isForced) {
        Set<String> alreadyDumpedGroups = new HashSet<>();
        log.info("Починаю локальне зберігання розкладів");
        UserDB.getAllUsers().values().forEach(x -> {
            String groupNumber = x.getGroup().getGroupName();
            if (groupNumber == null || alreadyDumpedGroups.contains(groupNumber)) return;
            try {
                log.info("Починаю локальне зберігання розкладів для користувача %d. Його група: %s".formatted(x.getChatID(), x.getGroup().getGroupName()));
                Path directoryPath = Paths.get("groupsSchedules");
                HashMap<String, String> params = x.getUserParams();
                String fileName = "%s(%d)_thisWeek.json".formatted(x.getGroup().getGroupName(), x.getGroup().getGroupNumber());
                prepareForWriting(directoryPath, fileName, params,
                        DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString())),
                        DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)),
                        isForced);
                fileName = "%s(%d)_nextWeek.json".formatted(x.getGroup().getGroupName(), x.getGroup().getGroupNumber());
                prepareForWriting(directoryPath, fileName, params,
                        DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)),
                        DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(14)), isForced);
                alreadyDumpedGroups.add(groupNumber);
            } catch (IOException e) {
                log.error("Виникла помилка під час спроби записати розклад у файл. Привід: %s".formatted(e.getMessage()));
            }
        });
    }

    private void prepareForWriting(Path directoryPath, String fileName, HashMap<String, String> params, String dateFrom, String dateTo, boolean isForced) throws IOException {
        if (checkIfAlreadyWritten(directoryPath, fileName, isForced)) {
            String response = "";
            params.put("dateFrom", dateFrom);
            params.put("dateTo", dateTo);
            response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
            writeFile(directoryPath, fileName, response);
            log.success("Файл %s записано успішно!".formatted(fileName));
        } else {
            log.error("Файл %s вже нещодавно був записаний. Пропускаю...".formatted(fileName));
        }
    }
    private boolean checkIfAlreadyWritten(Path directoryPath, String fileName, boolean isForced) {
        Path filePath = directoryPath.resolve(fileName);
        try {
            return isForced || DateUtils.instantToLocalDate(Files.getLastModifiedTime(filePath).toInstant()).isBefore(DateUtils.getTodayDate().minusDays(1)) || "Понеділок".equalsIgnoreCase(DateUtils.getDayOfWeek(DateUtils.getTodayDateString()));
        } catch (IOException exception) {
            log.error("Файла %s не існує. Починаємо запис...".formatted(fileName));
            return true;
        }
    }
    private void writeFile(Path directoryPath, String fileName, String data) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        Path filePath = directoryPath.resolve(fileName);
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile(), false)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }
}
