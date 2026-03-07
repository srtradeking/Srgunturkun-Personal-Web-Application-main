package com.webapp.backend.exception;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CustomErrorResponseSerializer {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(ErrorResponse errorResponse) {
        return gson.toJson(errorResponse);
    }
}
