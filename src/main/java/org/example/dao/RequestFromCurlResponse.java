package org.example.dao;

import java.util.Map;

public class RequestFromCurlResponse {

    private boolean isValidJson;
    private String testCaseName;
    private String testCasesBody;

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCasesBody(String testCasesBody) {
        this.testCasesBody = testCasesBody;
    }

    public String getTestCasesBody() {
        return testCasesBody;
    }

    public void setValidJson(boolean validJson) {
        isValidJson = validJson;
    }

    public boolean isValidJson() {
        return isValidJson;
    }
}
