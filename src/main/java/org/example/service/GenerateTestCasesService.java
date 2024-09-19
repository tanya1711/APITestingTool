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
        try {
            String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("api-version", "2023-05-15");

            String urlWithParams = builder.toUriString();
            BotRequest botRequest = new BotRequest(model,
                    List.of(new Message("system", tcResponse + " from the above table generate json request bodies for all test cases")),
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
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");

        String urlWithParams = builder.toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("Here is a sample JSON request body for a recruiter registration API. API is used to register a new recruiter to our portal. Email domain should be mailsac and name max char limit is 10\n");
        sb.append(request);
        sb.append("\n\nI want you to generate a comprehensive list of all possible values (both valid and invalid) for each field in this JSON request body.");
        sb.append("For each test case, only one field should be invalid at a time, while all other fields should be valid. This will help isolate which field is causing a failure.");
        sb.append("\nMake sure to cover all positive and negative test cases for thorough testing of the REST API.");
        sb.append("\n\nEach row in the list should represent a unique test case with different values for the fields.");
        sb.append("\nThe table should have columns for each field, and each row should contain values for all fields in that specific test case.");
        sb.append("\n\nNote: If I explicitly ask to exclude a specific field, do not generate test cases for it.");
        sb.append(" give top 10 test cases");

        BotRequest botRequest = new BotRequest(model,
                List.of(new Message("system", sb.toString())),
                maxCompletions,
                temperature,
                maxTokens,
                deploymentName);

        BotResponse response = restTemplate.postForObject(urlWithParams, botRequest, BotResponse.class);
        String testCases = response.getChoices().get(0).getMessage().getContent();
//        Thread.sleep(8000);
        String s = generateRequestBodiesFromTCs(testCases);
        if (s == null) {
            return new ArrayList<>();
        }

        return parseTcFromGptResponse(s);
    }

    public List<String> parseTcFromGptResponse(String gptResponse) {
        List<String> requestBodies = new ArrayList<>();
        System.out.println("gpt response");
        System.out.println(gptResponse);
        gptResponse = gptResponse.replaceAll("```", "").trim();
        String[] jsonParts = gptResponse.split(":\\s*\\n");
        for (String json : jsonParts) {
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                requestBodies.add(json);
                System.out.println(json);

            }
        }
        return requestBodies;
    }

}
