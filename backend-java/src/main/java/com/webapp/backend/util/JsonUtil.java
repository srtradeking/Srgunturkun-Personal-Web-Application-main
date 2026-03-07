package com.webapp.backend.util;

import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtil {

    public static Map<String, String> parseJson(String json) {
        return java.util.Arrays.stream(json.substring(1, json.length() - 1).split(","))
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(s -> s[0].replace("\"", "").trim(), s -> s[1].replace("\"", "").trim()));
    }
}
