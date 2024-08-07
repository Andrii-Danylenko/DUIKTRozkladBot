package org.rozkladbot.web;

import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.handlers.ResponseHandler;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.Callable;

@Component("Requester")
public class Requester {
    private final static RequestBuilder requestBuilder = new RequestBuilder();
    private final static ConsoleLineLogger<Requester> log = new ConsoleLineLogger<>(Requester.class);

    public static String makeRequest(String baseUrl, HashMap<String, String> params) throws IOException {
        log.info("Починаю створення запиту...");
        URL url = new URL(baseUrl + requestBuilder.getParameters(params));
        log.info("Створене посилання для запиту: %s".formatted(url));
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        log.info("Відкриваю з'єднання з сервером API...");
        return readRequest(connection);
    }
    private static String readRequest(HttpsURLConnection connection) throws IOException {
        log.info("Починаю зчитування відповіді сервера API...");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();
        log.success("Успішно зчитав дані з сервера!");
        return content.toString();
    }

    private static class RequestBuilder {
        public String getParameters(HashMap<String, String> params) {
            StringBuilder builder = new StringBuilder();
            params.forEach((key, value) -> builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&"));
            return builder.substring(0, builder.length() - 1);
        }
    }
}