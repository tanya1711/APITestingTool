package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RunTestCasesService {

    @Autowired
    private ContentService contentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String url = null;

    public static String extractUrl(String curlCommand) {
        Pattern urlPattern = Pattern.compile("curl\\s+'([^']+)'");
        Matcher matcher = urlPattern.matcher(curlCommand);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static Map<String, String> extractHeaders(String curlCommand) {
        Map<String, String> headers = new HashMap<>();
        Pattern headerPattern = Pattern.compile("--header\\s+'([^:]+):\\s([^']+)'");
        Matcher matcher = headerPattern.matcher(curlCommand);
        while (matcher.find()) {
            headers.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return headers;
    }

    public static String extractData(String curlCommand) {
        Pattern dataPattern = Pattern.compile("--data-raw\\s+'([^']+)'");
        Matcher matcher = dataPattern.matcher(curlCommand);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String createRequestPayload(String requestData) throws IOException {
        try {
            return objectMapper.readTree(requestData).toString();
        } catch (Exception e) {
            return null;
        }

    }

    private static void verifyRequestJson(String jsonPayload){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);
//            System.out.println("Parsed JSON: " + jsonNode.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String runTestApi(String curl, Object request) throws IOException {
        String testCaseString = request.toString();
        System.out.println(testCaseString);
        verifyRequestJson(testCaseString);
        String url = extractUrl(curl);
        Map<String, String> headers = extractHeaders(curl);
        return sendRequest(url, testCaseString, headers);
    }



    private static String sendRequest(String url, String requestPayload, Map<String, String> headers) throws IOException {
//        System.out.println("----------------------send request-----------------------");
//        System.out.println(requestPayload);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(url);
            StringEntity entity = new StringEntity(requestPayload, ContentType.APPLICATION_JSON);
            postRequest.setEntity(entity);

            // Set headers
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    postRequest.setHeader(header.getKey(), header.getValue());
                }
            }

            try (CloseableHttpResponse response = client.execute(postRequest)) {
                int statusCode = response.getCode();
                JsonNode responseBody = objectMapper.readTree(response.getEntity().getContent());
                System.out.println("Status code: " + statusCode);
                System.out.println("Response body: " + responseBody);
                ObjectMapper objectMapper = new ObjectMapper();
                String value = objectMapper.writeValueAsString(responseBody);
                return value;
            }
        }
    }
}