package org.example.dao.requestFromCurl.response;

public class TestRequestBodies {

    private String testCaseName;
    private boolean isValidJSON;
    private String testRequestBody;

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestRequestBody(String testRequestBody) {
        this.testRequestBody = testRequestBody;
    }

    public String getTestRequestBody() {
        return testRequestBody;
    }

    public void setValidJSON(boolean validJSON) {
        isValidJSON = validJSON;
    }

    public boolean isValidJSON() {
        return isValidJSON;
    }
}
