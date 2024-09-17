package org.example.service;

import jakarta.servlet.http.PushBuilder;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class RunTestCasesService {

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

    public String generateRequestBodiesFromTCs(String tcResponse){
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");

        String urlWithParams = builder.toUriString();
        BotRequest botRequest = new BotRequest(model,
                List.of(new Message("system", tcResponse+" from the above table generate json request bodies for all test cases")),
                maxCompletions,
                temperature,
                maxTokens,
                deploymentName);

        BotResponse response = restTemplate.postForObject(urlWithParams, botRequest, BotResponse.class);
        System.out.println(response.getChoices().get(0).getMessage().getContent());
        return response.getChoices().get(0).getMessage().getContent();
    }

//    public String getRequestBodyForTestCase(){
//
//    }
}
