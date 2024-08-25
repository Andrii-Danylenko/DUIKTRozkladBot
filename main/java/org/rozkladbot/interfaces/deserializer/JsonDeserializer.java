package org.rozkladbot.interfaces.deserializer;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Iterator;

public interface JsonDeserializer<V, T> extends DefaultDeserializer<V, T> {
    Iterator getJsonIterator(String dirName, String fileName, String jsonArrayName) throws IOException, ParseException;
}
