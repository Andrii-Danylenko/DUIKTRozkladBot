package org.rozkladbot.utils.schedule;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rozkladbot.entities.Classes;
import org.rozkladbot.entities.DayOfWeek;
import org.rozkladbot.entities.Table;
import org.rozkladbot.utils.date.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

public class ScheduleParser {

    private Iterator<JSONObject> getJsonFromServerResponse(String serverResponse) throws IOException, ParseException, org.json.simple.parser.ParseException {
        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(serverResponse);
        JSONArray jsonArray = (JSONArray) jsonObject.get("schedule");
        return jsonArray.iterator();
    }

    private Table parseData(Iterator<JSONObject> dataIterator, String startDate, String endDate) {
        TreeMap<LocalDate, List<Classes>> map = new TreeMap<>();
        while (dataIterator.hasNext()) {
            JSONObject obj = dataIterator.next();
            LocalDate date = DateUtils.parseDate(String.valueOf(obj.get("date")));
            int pairNumber = Integer.parseInt((String) obj.get("number"));
            String pairDetails = "[%s], каб. %s, %s".formatted(obj.get("type"), obj.get("cabinet"), obj.get("whoShort"));
            if (!map.containsKey(date)) {
                List<Classes> list = new ArrayList<>();
                list.add(new Classes(pairNumber, DateUtils.getPairTime(pairNumber + ""), pairDetails, (String) obj.get("name")));
                map.put(date, list);
            } else {
                map.get(date).add(new Classes(pairNumber, DateUtils.getPairTime(pairNumber + ""), pairDetails, (String) obj.get("name")));
            }
        }
        validateData(map, startDate, endDate);
        return getTable(map);
    }
    private Table getTable(TreeMap<LocalDate, List<Classes>> data) {
        Table table = new Table();
        for (Map.Entry<LocalDate, List<Classes>> entry:data.entrySet()) {
            DayOfWeek day = new DayOfWeek(entry.getKey(), DateUtils.getDayOfWeek(DateUtils.toString(entry.getKey())), entry.getValue());
            table.getTable().add(day);
        }
        return table;
    }
    private void validateData(TreeMap<LocalDate, List<Classes>> data, String startDate, String endDate) {
        LocalDate startD = DateUtils.parseDate(startDate);
        LocalDate endD = DateUtils.parseDate(endDate);
        for (LocalDate date = startD; !date.isAfter(endD); date = date.plusDays(1)) {
            data.computeIfAbsent(date, k -> new ArrayList<>());
        }
        data.entrySet().removeIf(entry -> entry.getKey().isBefore(startD) || entry.getKey().isAfter(endD));
    }
    private Table returnTableIfScheduleIsEmpty(String startDate, String endDate) {
        Table table = new Table();
        LocalDate startD = DateUtils.parseDate(startDate);
        LocalDate endD = DateUtils.parseDate(endDate);
        while (startD.isBefore(endD.plusDays(1))) {
            table.addDay(new DayOfWeek(startD, DateUtils.getDayOfWeek(DateUtils.toString(startD)), new ArrayList<>()));
            startD = startD.plusDays(1);
        }
        return table;
    }
    public Table getTable(String serverResponse, HashMap<String, String> params) {
        Table table = new Table();
        try {
            table = parseData(getJsonFromServerResponse(serverResponse),
                    params.get("dateFrom"), params.get("dateTo"));
        } catch (IOException | ParseException exception) {
            exception.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
           table = returnTableIfScheduleIsEmpty(params.get("dateFrom"), params.get("dateTo"));
        }
        return table;
    }
}