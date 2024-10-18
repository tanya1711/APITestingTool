package org.example.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class TestDataService {

    public Map<String, String> replaceFieldsFromRequestBody(Map<String, String> requestBodiesMap) {
        return replacePhoneFromRequestBody(replaceEmailFromRequestBody(requestBodiesMap));
    }

    public Map<String, String> replaceEmailFromRequestBody(Map<String, String> requestBodiesMap) {
        ObjectMapper mapper = new ObjectMapper();
        requestBodiesMap.forEach((String tcName, String tcBody) -> {
            if (!tcName.toLowerCase().contains("email")) {
                try {
                    JsonNode rootNode = mapper.readTree(tcBody);
                    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        JsonNode fieldValue = field.getValue();
                        if (fieldName.toLowerCase().contains("email")) {
                            String username = fieldValue.asText().split("@")[0];
                            String domain = fieldValue.asText().split("@")[1];
                            ((ObjectNode) rootNode).put(fieldName, username + System.nanoTime() + "@" + domain);
                        }
                    }
                    String updatedJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                    requestBodiesMap.replace(tcName, updatedJsonString);

                } catch (Exception e) {
                    System.err.println("Error processing JSON for key: " + tcName + " - " + e.getMessage());
                }

            }

        });
        return requestBodiesMap;

    }

    public Map<String, String> replacePhoneFromRequestBody(Map<String, String> requestBodiesMap) {
        ObjectMapper mapper = new ObjectMapper();
        requestBodiesMap.forEach((String tcName, String tcBody) -> {
            if (!tcName.toLowerCase().contains("phone")) {
                try {
                    JsonNode rootNode = mapper.readTree(tcBody);
                    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        if (fieldName.toLowerCase().contains("phone") || fieldName.toLowerCase().contains("mobile")) {
                            LocalDateTime now = LocalDateTime.now();
                            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("ddHH");
                            String formattedDate = now.format(dateFormatter);
                            long milliseconds = System.currentTimeMillis() % 1000;
                            Random random = new Random();
                            int randomTwoDigits = 10 + random.nextInt(90);
                            String result = formattedDate + milliseconds + randomTwoDigits;
                            String phone = 1 + result;
                            System.out.println("Date, Month, Hour, and Milliseconds: " + phone);

                            ((ObjectNode) rootNode).put(fieldName, phone);
                        }
                    }
                    String updatedJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                    requestBodiesMap.replace(tcName, updatedJsonString);

                } catch (Exception e) {
                    System.err.println("Error processing JSON for key: " + tcName + " - " + e.getMessage());
                }
            }

        });

        return requestBodiesMap;

    }

}
