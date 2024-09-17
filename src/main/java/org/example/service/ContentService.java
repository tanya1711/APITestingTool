package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.example.model.request.BotRequest;
import org.example.model.request.Message;
import org.example.model.response.BotResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ContentService {

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

    private HSSFSheet sheet;
    private HSSFWorkbook workbook;
    private ArrayList<ArrayList<String>> tcs = new ArrayList<>();

    public ContentService() {
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("Data");
    }

    public int parseContentForExcel(String content) throws IOException {
        String[] body = content.split("\\n");
        writeCasesToExcel(Arrays.asList(body));
        writeToXlsFile();
        return tcs.size();
    }

    public int getResponseColumnNumber() {
        int lastCell = 0;
        for (int i = 0; i < tcs.size(); i++) {
            HSSFRow hssfRow = sheet.getRow(i);
            int lastCellNum = hssfRow.getLastCellNum();
            lastCell = Math.max(lastCellNum, lastCell);
        }
        return lastCell;
    }

    public String generateJsonRequestFromTC(Integer tcId) throws JsonProcessingException {
        ArrayList<String> innerArray = tcs.get(tcId);
        StringBuilder requestBodyBuilder = new StringBuilder();
        boolean start = false;
//        boolean end = false;
        for (String i : innerArray) {
            if (start == true) {
                requestBodyBuilder.append(", ");
                requestBodyBuilder.append(i);
            }

            if (i.charAt(0) == '{') {
                requestBodyBuilder.append(i);
                start = true;
            }
        }
        String requestBody = requestBodyBuilder.toString().trim();
        String s = convertToAValidJsonString(requestBody);
//        ObjectMapper objectMapper = new ObjectMapper();
//        Object json = objectMapper.readValue(requestBody, Object.class);
//        String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
//        System.out.println("-------------------------------------------------------------------------------------------------------------------------");
//        System.out.println("printing " + formattedJson);
        return s;

    }


    public void writeCasesToExcel(List<String> body) {
        for (int i = 0; i < body.size(); i++) {
            ArrayList<String> innerArray = new ArrayList<>();
            HSSFRow row = sheet.createRow(i);
            HSSFCell cell;
            List<String> tcField = Arrays.asList(body.get(i).split("\\,"));
            for (int j = 0; j < tcField.size(); j++) {
                cell = row.createCell(j);
                cell.setCellValue(removeQuotes(tcField.get(j).trim()));
                innerArray.add(removeQuotes(tcField.get(j).trim()));
            }
            tcs.add(innerArray);
        }
    }

    public void writeResponseToExcel(String response, int row, int col) throws IOException {
        HSSFRow hssfRow = sheet.getRow(row);
        HSSFCell cell = hssfRow.createCell(col);
        cell.setCellValue(response);
        if (row > 0 && !response.equalsIgnoreCase("Invalid request")) {
            String responseMessage = getResponseMessage(response);
            if (responseMessage != null) {
                HSSFCell cell1 = hssfRow.createCell(col + 1);
                cell1.setCellValue(responseMessage);
            }
        } else if (!response.equalsIgnoreCase("Invalid request")) {
            HSSFCell cell1 = hssfRow.createCell(col + 1);
            cell1.setCellValue("Response Message");
        }
        writeToXlsFile();
    }

    public String getResponseMessage(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode messageNode = jsonNode.path("message");
            message = messageNode.isMissingNode() ? null : messageNode.asText();
            System.out.println("Message: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

    public static String removeQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str; // handle empty string
        }

        int length = str.length();
        if (length < 2 || str.charAt(0) != '"' || str.charAt(length - 1) != '"') {
            return str; // no quotes or invalid format
        }

        return str.substring(1, length - 1); // remove first and last character (quotes)
    }

    public void createExcelSheetAndSetTitles(List<String> headings) throws IOException {
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("Data");
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell;
        for (int i = 0; i < headings.size(); i++) {
            cell = row.createCell(i);
            cell.setCellValue(headings.get(i).trim());
        }
    }

    public void writeToXlsFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/dataFiles/data.xls");
        workbook.write(fos);
    }

    public String convertToAValidJsonString(String requestBody) {
        String prompt = requestBody + "  , convert this to a valid json string";
        String url = baseUrl + "openai/deployments/" + deploymentName + "/chat/completions";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api-version", "2023-05-15");
        String urlWithParams = builder.toUriString();
        BotRequest request = new BotRequest(model,
                List.of(new Message("system", prompt)),
                maxCompletions,
                temperature,
                maxTokens,
                deploymentName);

        BotResponse response = restTemplate.postForObject(urlWithParams, request, BotResponse.class);

        return response.getChoices().get(0).getMessage().getContent();

    }


}

