package org.rozkladbot.utils.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rozkladbot.interfaces.deserializer.JsonDeserializer;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractJsonDeserializer<V, T> implements JsonDeserializer<V, T> {
    protected JSONParser jsonParser = new JSONParser();

    @Override
    public abstract Map<V, T> deserialize(String dirName, String fileName, String jsonArrayName, String...keys) throws IOException, ParseException;

    protected abstract void getObjectAndApply(JSONObject jsonObject, String key, T entity);
    public Iterator getJsonIterator(String dirName, String fileName, String jsonArrayName) throws IOException, ParseException {
        Path directoryPath = Paths.get(dirName);
        Path filePath = directoryPath.resolve(fileName);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(String.valueOf(filePath)));
        JSONArray jsonArray = (JSONArray) jsonObject.get(jsonArrayName);
        return jsonArray.iterator();
    }
}
