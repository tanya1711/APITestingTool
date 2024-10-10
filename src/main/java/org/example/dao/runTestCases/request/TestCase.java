package org.example.dao.runTestCases.request;

public class TestCase {

    private String tcId;
    private String testRequestBody;

    public void setTcId(String tcId) {
        this.tcId = tcId;
    }

    public String getTcId() {
        return tcId;
    }

    public void setTestRequestBody(String testRequestBody) {
        this.testRequestBody = testRequestBody;
    }

    public String getTestRequestBody() {
        return testRequestBody;
    }
}
