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
        BotRequest botRequest = new BotRequest(model,
                List.of(new Message("system", request + " Generate importrant field specifications table for above api.  Don't change the field - id in request. String char limit min = 5 max = 20")),
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
