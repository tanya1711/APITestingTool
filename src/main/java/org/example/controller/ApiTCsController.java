package org.example.controller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import org.example.dao.runTestCases.CurlAndDescriptionRequest;
import org.example.dao.requestFromCurl.response.TestRequestBodies;
import org.example.dao.runTestCases.request.RunTestCaseRequest;
import org.example.dao.runTestCases.request.TestCase;
import org.example.dao.runTestCases.response.TestResult;
import org.example.service.ContentService;
import org.example.service.GenerateTestCasesService;
import org.example.service.RunTestCasesService;
import org.example.util.ValidateJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/generateDescription")
    public ResponseEntity<?> getDescription(@RequestBody String curlRequest) throws IOException, InterruptedException {
        String descriptionForCurl = generateTestCasesService.generateDescriptionForCurl(curlRequest);
        return ResponseEntity.ok(descriptionForCurl);
    }

    @PostMapping(value = "/requestFromCurl")
    public ResponseEntity<?> getRequestFromCurl(@RequestBody CurlAndDescriptionRequest curlAndDescriptionRequest) throws IOException, InterruptedException {
        Map<String, String> tcRequestBodies = generateTestCasesService.generateTestCasesForCurl(curlAndDescriptionRequest);
        List<TestRequestBodies> testRequestBodiesList = new ArrayList<>();
        tcRequestBodies.forEach((String tcName, String testBody) -> {
            TestRequestBodies testRequestBodies = new TestRequestBodies();
            String s = ValidateJSON.validateAndCorrectJSON(testBody);
            if (s == null) {
                testRequestBodies.setValidJSON(false);
                testRequestBodies.setTestRequestBody(testBody);
                testRequestBodies.setTestCaseName(tcName);
            } else {
                testRequestBodies.setValidJSON(true);
                testRequestBodies.setTestRequestBody(s);
                testRequestBodies.setTestCaseName(tcName);
            }
            testRequestBodiesList.add(testRequestBodies);
        });
        return ResponseEntity.ok(testRequestBodiesList);
    }

    @PostMapping(value = "/runTestCase")
    public ResponseEntity<?> generateResponse(@RequestBody RunTestCaseRequest runTestCaseRequest) throws IOException, InterruptedException {
        List<TestResult> answer = new ArrayList<>();
        for (int i = 0; i < runTestCaseRequest.getRequestBodyList().size(); i++) {
            TestResult testResult = new TestResult();
            TestCase testCase = runTestCaseRequest.getRequestBodyList().get(i);
            String s = runTestCasesService.runTestApi(runTestCaseRequest.getCurl(), testCase.getTestRequestBody());
            testResult.setTcId(testCase.getTcId());
            System.out.println(s);
            testResult.setTcResponse(s.split("\\|", 2)[1]);
            testResult.setStatusCode(s.split("\\|", 2)[0]);

            answer.add(testResult);
        }

        return ResponseEntity.ok(answer);
    }
}
