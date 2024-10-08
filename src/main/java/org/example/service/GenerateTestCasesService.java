package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.PushBuilder;
import org.example.dao.runTestCases.CurlAndDescriptionRequest;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.example.util.ValidateJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
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

    public String getRequestBodyFromCurl(String curlCommand) {
        Pattern bodyPattern = Pattern.compile(
                "-d\\s+'([^']*)'|-d\\s+\"([^\"]*)\"|--data\\s+'([^']*)'|--data\\s+\"([^\"]*)\"|--data-raw\\s+'([^']*)'|--data-raw\\s+\"([^\"]*)\""
        );
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

    public Map<String, String> generateTestCasesForCurl(CurlAndDescriptionRequest curlAndDescriptionRequest) {
        String request = getRequestBodyFromCurl(curlAndDescriptionRequest.getCurl());
        String description = curlAndDescriptionRequest.getDescription();
        System.out.println(request);

        String dataTypesOfAllFieldsFromRequest = getDataTypesOfAllFieldsFromRequest(request);
//        String dataTypesOfAllFieldsFromRequest = request;
        StringBuilder sb = new StringBuilder();
        sb.append("Here is a sample JSON request body for the data entry  API.  \n");
        sb.append(request);
        sb.append(description);
        sb.append("\n\nI want you to generate a comprehensive list of all possible values (both valid and invalid) for each field in this JSON request body.");
        sb.append("For each test case, only one field should be invalid at a time, while all other fields should be valid. This will help isolate which field is causing a failure.");
        sb.append("\nMake sure to cover all positive and negative test cases for thorough testing of the REST API.");
        sb.append("\n\nEach row in the list should represent a unique test case with different values for the fields.");
        sb.append("\nThe table should have columns for the Test Case Name, each field, and for nested objects, use dot notation for proper representation, and each row should contain values for all fields in that specific test case.");
        sb.append("\n\nNote: If I explicitly ask to exclude a specific field, do not generate test cases for it.");
//        sb.append(" give top 5 test cases");

        String testCases = botService.getChatGPTResponseForPrompt(sb.toString());
        System.out.println(testCases);
        List<String> rows = storeTableRowsToList(testCases);
        Map<String, String> requestBodiesMap = new HashMap<>();
        for (int i = 2; i < rows.size(); i++) {

            String s = generateRequestBodiesFromTCs(rows.get(0) + "\n" + rows.get(i), dataTypesOfAllFieldsFromRequest);
//            System.out.println(s);
            String tcName = rows.get(i).split("\\|")[0];
            System.out.println(tcName);
            System.out.println(s);
            requestBodiesMap.put(tcName, s);
//            requestBodiesMap.put(tcName," { "+ s.split("\\{", 2)[1]);
//            String str = requestBodiesMap.get(tcName);
//            int lastIndex = str.lastIndexOf('}');
//            requestBodiesMap.replace(tcName, str.substring(0, lastIndex+1));

        }
        System.out.println(requestBodiesMap);
        return requestBodiesMap;
    }

    public String generateDescriptionForCurl(String curl) {
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

    public String parseTcFromGptResponse(String gptResponse) {
        ArrayList<String> jsonBodies = new ArrayList<>();
        Pattern pattern = Pattern.compile("```\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(gptResponse);
        while (matcher.find()) {
            jsonBodies.add(matcher.group(1).trim());
        }
//        System.out.println(jsonBodies);
        return gptResponse;
    }


}
