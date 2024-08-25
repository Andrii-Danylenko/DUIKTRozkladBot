package org.rozkladbot.utils.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.rozkladbot.entities.Group;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupUtils extends AbstractJsonDeserializer<String, Group> {
    @Override
    public Map<String, Group> deserialize(String dirName, String fileName, String jsonArrayName, String... keys) throws IOException, ParseException {
            Iterator jsonArrayIterator = getJsonIterator(dirName, fileName, jsonArrayName);
            JSONObject jsonObject;
            Map<String, Group> groupMap = new ConcurrentHashMap<>();
            Group group;
            while (jsonArrayIterator.hasNext()) {
                group = new Group();
                jsonObject = (JSONObject) jsonArrayIterator.next();
                for (String key : keys) {
                    getObjectAndApply(jsonObject, key, group);
                }
                groupMap.put(group.getGroupName(), group);
            }
            return groupMap;
    }

    @Override
    protected void getObjectAndApply(JSONObject jsonObject, String key, Group entity) {
        switch (key) {
            case "institute" -> entity.setInstitute((String) jsonObject.get("institute"));
            case "group" -> entity.setGroupName((String) jsonObject.get("group"));
            case "faculty" -> entity.setFaculty((String) jsonObject.get("faculty"));
            case "groupNumber" -> entity.setGroupNumber((long) jsonObject.get("groupNumber"));
            case "course" -> entity.setCourse((String) jsonObject.get("course"));
        }
    }
}
