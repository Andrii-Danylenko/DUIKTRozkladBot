package org.rozkladbot.interfaces.deserializer;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Map;

public interface DefaultDeserializer<V, T> {
    Map<V, T> deserialize(String dirName, String fileName, String jsonArrayName, String...keys) throws IOException, ParseException;
}
