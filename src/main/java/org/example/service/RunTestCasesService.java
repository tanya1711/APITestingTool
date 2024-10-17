package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
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
        Pattern urlPattern = Pattern.compile("curl\\s*'?([^'\\s]+)'?");
        Matcher matcher = urlPattern.matcher(curlCommand);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractMethodFromCurl(String curlCommand) {
        if (curlCommand.contains("PUT")) {
            return "PUT";
        }
        // Check if the curl command contains '-d' or '--data' (which indicates POST)
        else {
            return "POST";
        }
    }

    public static Map<String, String> extractHeaders(String curlCommand) {
        Map<String, String> headers = new HashMap<>();
        Pattern headerPattern = Pattern.compile("(?:-H|--header)\\s*'([^:]+):\\s*([^']+)'");
        Matcher matcher = headerPattern.matcher(curlCommand);
        while (matcher.find()) {
            headers.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        System.out.println("headers " + headers);
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

    private static void verifyRequestJson(String jsonPayload) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);
//            System.out.println("Parsed JSON: " + jsonNode.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String runTestApi(String curl, String request) throws IOException {
        System.out.println("curl");
        System.out.println(curl);
        verifyRequestJson(request);
        String url = extractUrl(curl);
        String method = extractMethodFromCurl(curl);
        System.out.println(method);
        Map<String, String> headers = extractHeaders(curl);
        System.out.println(headers);
        return sendRequest(url, request, headers, method);
    }


    private static String sendRequest(String url, String requestPayload, Map<String, String> headers, String method) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            if (method.contains("PUT")) {
                HttpPut putRequest = new HttpPut(url);
                StringEntity entity = new StringEntity(requestPayload, ContentType.APPLICATION_JSON);
                putRequest.setEntity(entity);

                if (headers != null && !headers.isEmpty()) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        putRequest.setHeader(header.getKey(), header.getValue());
                    }
                }

                try (CloseableHttpResponse response = client.execute(putRequest)) {
                    int statusCode = response.getCode();
                    JsonNode responseBody = objectMapper.readTree(response.getEntity().getContent());
                    System.out.println("Status code: " + statusCode);
                    System.out.println("Response body: " + responseBody);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String value = objectMapper.writeValueAsString(responseBody);

                    return statusCode + "|" + value;
                }
            } else {
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
                    return statusCode + "|" + value;

                }
            }
        }
    }
}