package org.example.controller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.request.TestData;
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
import java.util.ArrayList;
import java.util.List;

@RestController
public class ApiTCsController {

    @Autowired
    private RunTestCasesService runTestCasesService;

    @Autowired
    private GenerateTestCasesService generateTestCasesService;

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
    public ResponseEntity<?> getRequestFromCurl(@RequestBody String curlRequest) throws IOException, InterruptedException {
        List<String> tcRequestBodies = generateTestCasesService.generateTestCasesForCurl(curlRequest);
        return ResponseEntity.ok(tcRequestBodies);
    }

    @PostMapping(value = "/runTestCase")
    public ResponseEntity<?> generateResponse(@RequestBody TestData testData) throws IOException, InterruptedException {
        List<String> answer = new ArrayList<>();
        for (int i = 0; i < testData.getTestcases().size(); i++) {
            String s = runTestCasesService.runTestApi(testData.getCurl(), testData.getTestcases().get(i));
            answer.add(s);
        }

        return ResponseEntity.ok(answer);
    }


}
