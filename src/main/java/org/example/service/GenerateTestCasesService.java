package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.PushBuilder;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GenerateTestCasesService {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-completions}")
    private int maxCompletions;

    @Value("${openai.temperature}")
    private double temperature;

    @Value("${openai.max_tokens}")
    private int maxTokens;

    @Value("${openai.api.baseUrl}")
    private String baseUrl;

    @Value("${openai.deployment.name}")
    private String deploymentName;

    @Autowired
    private RestTemplate restTemplate;


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


    public String generateRequestBodiesFromTCs(String tcResponse) {
        System.out.println(tcResponse);
        try {
            String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("api-version", "2023-05-15");

            String urlWithParams = builder.toUriString();
            BotRequest botRequest = new BotRequest(model,
                    List.of(new Message("system", tcResponse + " from the above table row generate a json request body for this test cases. Here is a sample request body to refer the nodes of API ")),
                    maxCompletions,
                    temperature,
                    maxTokens,
                    deploymentName);

            BotResponse response = restTemplate.postForObject(urlWithParams, botRequest, BotResponse.class);
            return response.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            System.out.println("Sorry token limit reached!!");
            return null;
        }
    }

    public List<String> generateTestCasesForCurl(String curl) {
        String request = getRequestBodyFromCurl(curl);
        System.out.println(request);
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");

        String urlWithParams = builder.toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("Here is a sample JSON request body for the data entry  API. This API is to register a new recruiter to our platform. For every test phoneNumber is equal to 2030219322 and email equal to testrecruiter_12911229@mailsac.com.  \n");
        sb.append(request);
        sb.append("\n\nI want you to generate a comprehensive list of all possible values (both valid and invalid) for each field in this JSON request body.");
        sb.append("For each test case, only one field should be invalid at a time, while all other fields should be valid. This will help isolate which field is causing a failure.");
        sb.append("\nMake sure to cover all positive and negative test cases for thorough testing of the REST API.");
        sb.append("\n\nEach row in the list should represent a unique test case with different values for the fields.");
        sb.append("\nThe table should have columns for each field, and each row should contain values for all fields in that specific test case.");
        sb.append("\n\nNote: If I explicitly ask to exclude a specific field, do not generate test cases for it.");
//        sb.append(" give top 5 test cases");

        BotRequest botRequest = new BotRequest(model,
                List.of(new Message("system", sb.toString())),
                maxCompletions,
                temperature,
                maxTokens,
                deploymentName);

        BotResponse response = restTemplate.postForObject(urlWithParams, botRequest, BotResponse.class);
        String testCases = response.getChoices().get(0).getMessage().getContent();
        List<String> rows = storeTableRowsToList(testCases);
        List<String> requestBodies = new ArrayList<>();
        for (int i = 2; i < rows.size(); i++) {
            String s = generateRequestBodiesFromTCs(rows.get(0) + "\n" + rows.get(i));
            requestBodies.add(s);
//            System.out.println(s);
        }
        System.out.println(requestBodies);
        return requestBodies;
    }

    public String generateDescriptionForCurl(String curl) {
        String request = getRequestBodyFromCurl(curl);
        System.out.println(request);
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");

        String urlWithParams = builder.toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("Describe the request body with each node field type and validations.  \n");
        sb.append(request);
        BotRequest botRequest = new BotRequest(model,
                List.of(new Message("system", sb.toString())),
                maxCompletions,
                temperature,
                maxTokens,
                deploymentName);

        BotResponse response = restTemplate.postForObject(urlWithParams, botRequest, BotResponse.class);
        String description = response.getChoices().get(0).getMessage().getContent();
        return description;
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
