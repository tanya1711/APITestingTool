package org.example.controller;

import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.example.service.ContentService;
import org.example.service.GenerateTestCasesService;
import org.example.service.RunTestCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@RestController
public class ApiTCsController {

    @Autowired
    private GenerateTestCasesService generateTestCasesService;

    @Autowired
    private RunTestCasesService runTestCasesService;

    @Autowired
    private RestTemplate restTemplate;

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
    private ContentService contentService;


    @PostMapping(value = "/requestFromCurl")
    public ResponseEntity<?> getRequestFromCurl(@RequestBody String curlRequest) throws IOException {
        String request = generateTestCasesService.getRequestBodyFromCurl(curlRequest);
        System.out.println(request);
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");

        String urlWithParams = builder.toUriString();
        StringBuilder sb = new StringBuilder();
        sb.append("Here is a sample JSON request body for a REST API:\n");
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
        System.out.println(testCases);
        String s = runTestCasesService.generateRequestBodiesFromTCs(testCases);
        return ResponseEntity.ok(s);
    }
}
