package org.opencds.cqf.test.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonUtils {

    public static Gson gson = new GsonBuilder().create();

    public static JsonObject getJsonObjectFromString(String json) {
        return gson.fromJson(json, JsonObject.class);
    }
    public static JsonObject getJsonObjectFromInputStream(InputStream json) {
        return gson.fromJson(new InputStreamReader(json), JsonObject.class);
    }

}
