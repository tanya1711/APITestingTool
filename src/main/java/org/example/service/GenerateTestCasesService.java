package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.PushBuilder;
import org.example.dao.runTestCases.CurlAndDescriptionRequest;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GenerateTestCasesService {

    @Autowired
    private BotService botService;

    @Autowired
    private TestDataService testDataService;

    public String getRequestBodyFromCurl(String curlCommand) {
        System.out.println("---------------------------------------------------CURLLLL-------------------------------");
        System.out.println(curlCommand);
        System.out.println("------------------------------------------------------------------------------------------");

        Pattern bodyPattern = Pattern.compile("-d\\s+'([^']*)'|-d\\s+\"([^\"]*)\"|--data\\s+'([^']*)'|--data\\s+\"([^\"]*)\"|--data-raw\\s+'([^']*)'|--data-raw\\s+\"([^\"]*)\"|--data-raw\\s+\\$'(.*)'", Pattern.DOTALL);
        Matcher matcher = bodyPattern.matcher(curlCommand);
        if (matcher.find()) {
            String body = null;
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    body = matcher.group(i);
                    break;
                }
            }
            if (body != null) {
                return body.trim();
            } else {
                System.out.println("No request body found.");
            }
        } else {
            System.out.println("No request body found.");
        }
        return null;
    }


    public String generateRequestBodiesFromTCs(String tcResponse, String requestBody) {
        try {
            String prompt = tcResponse + " from the above table row generate a json request body for this test case. DataTypes for all the fields are " + requestBody;
            return botService.getChatGPTResponseForPrompt(prompt);
        } catch (Exception e) {
            System.out.println("Sorry token limit reached!!");
            return null;
        }
    }

    public Map<String, String> generateTestCasesForCurl(CurlAndDescriptionRequest curlAndDescriptionRequest) throws InterruptedException {
        String request = getRequestBodyFromCurl(curlAndDescriptionRequest.getCurl());
        String description = curlAndDescriptionRequest.getDescription();
        System.out.println(request);

        String dataTypesOfAllFieldsFromRequest = getDataTypesOfAllFieldsFromRequest(request);
        StringBuilder sb = new StringBuilder();
        sb.append("Here is a sample JSON request body for the data entry API.  \n");
        sb.append(request);
        sb.append(description);
        sb.append("\n\nI want you to generate a comprehensive list of all possible values (both valid and invalid) for **every field**, including both top-level and nested fields, in this JSON request body.");
        sb.append("For each test case, only one field should be invalid at a time, while all other fields should be valid. This will help isolate which field is causing a failure.");
        sb.append("\nMake sure to cover all the possible positive and negative test cases for thorough testing of the REST API, including **nested fields** like those inside objects (e.g., company.jsCompanyId).");
        sb.append("\n\nEach row in the list should represent a unique test case with different values for all the fields.");
        sb.append("\nThe table should have columns for the Test Case Name and each field. For **nested objects**, use dot notation for proper representation, like company.company and company.jsCompanyId. Each row should contain values for all fields in that specific test case.");
        sb.append("\n\nNote: If I explicitly ask to exclude a specific field, do not generate test cases for it.");

        String testCases = botService.getChatGPTResponseForPrompt(sb.toString());
        System.out.println(testCases);
        List<String> rows = storeTableRowsToList(testCases);
        Map<String, String> requestBodiesMap = new LinkedHashMap<>();
        for (int i = 2; i < rows.size(); i++) {
            String s = generateRequestBodiesFromTCs(rows.get(0) + "\n" + rows.get(i), dataTypesOfAllFieldsFromRequest);
            String tcName = "";
            if (rows.get(i).charAt(0) == '|') {
                tcName = rows.get(i).split("\\|")[1].split("\\|")[0];
            } else {
                tcName = rows.get(i).split("\\|")[0];

            }
            requestBodiesMap.put(tcName, s);
        }

        for (int i = 2; i < rows.size(); i++) {
            String tcName = "";
            if (rows.get(i).charAt(0) == '|') {
                tcName = rows.get(i).split("\\|")[1].split("\\|")[0];
            } else {
                tcName = rows.get(i).split("\\|")[0];

            }
            if (tcName.contains("Valid") && !tcName.contains("Invalid") && requestBodiesMap.containsKey(tcName)) {
                String s = requestBodiesMap.get(tcName);
                requestBodiesMap.remove(tcName);
                requestBodiesMap.put(tcName, s);
            }
        }
        System.out.println(requestBodiesMap);
        return testDataService.replaceFieldsFromRequestBody(requestBodiesMap);
    }

//    public String changingValuesOfRequestBody(String request) { TODO: WILL BE REMOVED
//        StringBuilder sb = new StringBuilder();
//        sb.append("Given the following JSON request body: " + request + " \n\nPlease change all the field values to different valid values, ensuring they follow the same structure and format. Return the modified JSON with new values.");
//        return botService.getChatGPTResponseForPrompt(sb.toString());
//    }

    public String generateDescriptionForCurl(String curl) throws InterruptedException {
        String request = getRequestBodyFromCurl(curl);
        System.out.println(request);
        StringBuilder sb = new StringBuilder();
        sb.append("Describe the request body with each node field type and validations.  \n");
        sb.append(request);
        String description = botService.getChatGPTResponseForPrompt(sb.toString());
        return description;
    }

    public String getDataTypesOfAllFieldsFromRequest(String request) {
        String chatGPTResponseForPrompt = botService.getChatGPTResponseForPrompt(request + " For the provided JSON request body extract all the fields and there field types");
        System.out.println(chatGPTResponseForPrompt);
        return chatGPTResponseForPrompt;
    }

    public List<String> storeTableRowsToList(String testCases) {
        List<String> testCasesList = Arrays.asList(testCases.split("\n"));
        return testCasesList;
    }


}
