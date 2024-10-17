package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidateJSON {

    public static String validateAndCorrectJSON(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.readTree(jsonString);
            System.out.println("Valid JSON");
            return jsonString;
        } catch (JsonProcessingException e) {
            System.out.println("Invalid JSON: " + e.getMessage());
            String correctedJson = autoCorrectJson(jsonString);

            try {
                objectMapper.readTree(correctedJson);
                System.out.println("Auto-corrected JSON is valid");
                return correctedJson;
            } catch (JsonProcessingException correctedException) {
                System.out.println("Auto-correction failed: " + correctedException.getMessage());
                return null;
            }
        }
    }

    private static String autoCorrectJson(String jsonString) {
        return jsonString
                .replaceAll("([{,])\\s*([a-zA-Z0-9_]+)\\s*:", "$1\"$2\":")  // Add quotes around keys
                .replaceAll(",\\s*([}\\]])", "$1")  // Remove trailing commas
                .replaceAll("(\"[a-zA-Z0-9_]+\"\\s*:\\s*[^,}\\]]+)(?=\\s*\"[a-zA-Z0-9_]+\"\\s*:)", "$1,");  // Add missing commas
    }



}
