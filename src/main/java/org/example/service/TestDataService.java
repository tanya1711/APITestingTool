package org.example.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Service
public class TestDataService {

    public Map<String , String> replaceEmailFromRequestBody(Map<String, String> requestBodiesMap){
        ObjectMapper mapper = new ObjectMapper();
        requestBodiesMap.forEach((String tcName , String tcBody) ->{
            if(!tcName.toLowerCase().contains("email")){
                try {
                    JsonNode rootNode = mapper.readTree(tcBody);
                    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                            JsonNode fieldValue = field.getValue();
                            if (fieldName.toLowerCase().contains("email")) {
                                String username = fieldValue.asText().split("@")[0];
                                String domain = fieldValue.asText().split("@")[1];
                                ((ObjectNode) rootNode).put(fieldName, username+System.nanoTime()+"@"+domain);
                            }
                    }
                    String updatedJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                    requestBodiesMap.replace(tcName, updatedJsonString);

                } catch (Exception e) {
                    System.err.println("Error processing JSON for key: " + tcName + " - " + e.getMessage());
                }
            }

        });

        return requestBodiesMap;

    }

}
